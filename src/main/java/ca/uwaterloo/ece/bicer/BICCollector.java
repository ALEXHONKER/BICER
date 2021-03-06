package ca.uwaterloo.ece.bicer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.data.DeletedLineInCommits;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class BICCollector {

	public static void main(String[] args) {
		new BICCollector().run(args);
	}

	private String gitURI;
	private String pathToBuggyIDs;
	private boolean help;
	//private boolean verbose;
	private Date startDate;
	private Date endDate;
	private ArrayList<String> bugIDs;

	private Git git;
	private Repository repo;

	public void run(String[] args) {
		Options options = createOptions();

		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}

			// (1) load BugsIDs
			bugIDs = Utils.getLines(pathToBuggyIDs, false);

			String bugIDPreFix = bugIDs.get(0).split("-")[0] + "-";

			// (2) get commits between the start and end dates
			ArrayList<RevCommit> commits = getRevCommits();

			repo = git.getRepository();

			// (3) get deleted lines from commits. This data are used to identify BI lines that are only deleted and are in INSERT hunks in bug-fixing commits.
			HashMap<String,ArrayList<DeletedLineInCommits>> mapDeletedLines = getDeletedLinesInCommits(commits);

			// (4) find bug-fixing commits and get BI lines
			ArrayList<BIChange> lstBIChanges = new ArrayList<BIChange>();
			for(RevCommit rev:commits){
				String message = rev.getFullMessage();

				// Create matcher on file
				Pattern pattern = Pattern.compile(bugIDPreFix + "\\d+");
				Matcher matcher = pattern.matcher(message);

				while(matcher.find()){
					if(bugIDs.contains(matcher.group(0))){

						// Now it's a bug-fixing commit!

						// get a list of files in the commit
						RevCommit parent = rev.getParent(0);
						if(parent==null){
							System.err.println("WARNING: Parent commit does not exist: " + rev.name() );
							continue;
						}

						DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
						df.setRepository(repo);
						df.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
						df.setDetectRenames(true);
						List<DiffEntry> diffs;
						try {

							// do diff
							diffs = df.scan(parent.getTree(), rev.getTree());
							for (DiffEntry diff : diffs) {
								ArrayList<Integer> lstIdxOfDeletedLines = new ArrayList<Integer>();
								ArrayList<Integer> lstIdxOfOnlyInsteredLines = new ArrayList<Integer>();
								String oldPath = diff.getOldPath();
								String newPath = diff.getNewPath();

								// ignore Test files and non-java files.
								if(newPath.indexOf("Test")>=0 || !newPath.endsWith(".java")) continue;

								String id =  rev.name() + "";

								String origPrvFileSource = Utils.fetchBlob(repo, id +  "~1", oldPath);
								String origFileSource = Utils.fetchBlob(repo, id, newPath);

								String prevFileSource=Utils.removeLineComments(origPrvFileSource);
								String fileSource=Utils.removeLineComments(origFileSource);

								//String[] arrPrevFileSource = prevFileSource.split("\n");
								//String[] arrFileSource = fileSource.split("\n");


								EditList editList = Utils.getEditListFromDiff(prevFileSource, fileSource);

								// get line indices that are related to BI lines.
								for(Edit edit:editList){

									if(edit.getType()!=Edit.Type.INSERT){

										int beginA = edit.getBeginA();
										int endA = edit.getEndA();

										for(int i=beginA; i < endA ; i++)
											lstIdxOfDeletedLines.add(i);

									}else{
										int beginB = edit.getBeginB();
										int endB = edit.getEndB();

										for(int i=beginB; i < endB ; i++)
											lstIdxOfOnlyInsteredLines.add(i);
									}
								}

								// get BI commit from lines in lstIdxOfOnlyInsteredLines
								lstBIChanges.addAll(getBIChangesFromBILineIndices(id,rev.getCommitTime(), newPath, oldPath, origPrvFileSource,prevFileSource,lstIdxOfDeletedLines));
								lstBIChanges.addAll(getBIChangesFromDeletedBILine(id,rev.getCommitTime(),mapDeletedLines,origFileSource,fileSource,lstIdxOfOnlyInsteredLines,oldPath));
							}


							// get the previous commit and do blame for deleted lines in the previous commit

							// 
						} catch (IOException e) {
							e.printStackTrace();
						}
						df.close();
					}
				}
			}
			for(BIChange biChange:lstBIChanges)
				System.out.println(biChange.toString());
		}
	}

	private ArrayList<BIChange> getBIChangesFromDeletedBILine(String fixSha1, int fixCommitTime,
			HashMap<String, ArrayList<DeletedLineInCommits>> mapDeletedLines, String origFileSource, String fileSource,
			ArrayList<Integer> lstIdxOfOnlyInsteredLines, String oldPath) {
		
		ArrayList<BIChange> biChanges = new ArrayList<BIChange>();
		
		ArrayList<Integer> arrIndicesInOriginalFileSource = getOriginalLineIndices(origFileSource,fileSource,lstIdxOfOnlyInsteredLines);
		
		String[] arrOrigFileSource = origFileSource.split("\n");
		for(int lineNum:arrIndicesInOriginalFileSource){
			String key = arrOrigFileSource[lineNum].trim();
			ArrayList<DeletedLineInCommits> lstDeletedLines = mapDeletedLines.get(key);
			
			DeletedLineInCommits deletedLineToConsider = null;
			for(DeletedLineInCommits deletedLine:lstDeletedLines){
				if(deletedLine.getBIDate().compareTo(Utils.getStringDateTimeFromCommitTime(fixCommitTime))<=0
						&& deletedLine.getPath().equals(oldPath)){
					deletedLineToConsider = deletedLine;
				}
			}
			if(deletedLineToConsider==null)
				return biChanges;
			else{
				// get BIChange from the deleted line
				// TODO
			}
		}
		
		return biChanges;
	}

	private ArrayList<BIChange> getBIChangesFromBILineIndices(String fixSha1,int fixCommitTime, String path, String prevPath, String origPrvFileSource,
			String prevFileSource, ArrayList<Integer> lstIdxOfDeletedLines) {

		ArrayList<BIChange> biChanges = new ArrayList<BIChange>();

		// do Blame
		BlameCommand blamer = new BlameCommand(repo);
		ObjectId commitID;
		try {
			commitID = repo.resolve(fixSha1 + "~1");
			blamer.setStartCommit(commitID);
			blamer.setFilePath(prevPath);
			BlameResult blame = blamer.setTextComparator(RawTextComparator.WS_IGNORE_ALL).setFollowFileRenames(true).call();

			ArrayList<Integer> arrIndicesInOriginalFileSource = getOriginalLineIndices(origPrvFileSource,prevFileSource,lstIdxOfDeletedLines);
			for(int lineIndex:arrIndicesInOriginalFileSource){
				RevCommit commit = blame.getSourceCommit(lineIndex);
				String line = origPrvFileSource.split("\n")[lineIndex].trim();
				if(line.length()<2) // heuristic: ignore "}"
					continue;
				
				String BISha1 = commit.name();
				String biPath = blame.getSourcePath(lineIndex);
				//String path;
				String FixSha1 = fixSha1;
				String BIDate = Utils.getStringDateTimeFromCommitTime(commit.getCommitTime());
				String FixDate = Utils.getStringDateTimeFromCommitTime(fixCommitTime);
				int lineNum = blame.getSourceLine(lineIndex)+1;
				int lineNumInPrevFixRev = lineIndex+1;
				
				BIChange biChange = new BIChange(BISha1,biPath,FixSha1,path,BIDate,FixDate,lineNum,lineNumInPrevFixRev, origPrvFileSource.split("\n")[lineIndex].trim(),true);
				biChanges.add(biChange);
			}

		} catch (RevisionSyntaxException | IOException | GitAPIException e) {
			e.printStackTrace();
		}

		return biChanges;
	}

	private ArrayList<Integer> getOriginalLineIndices(String origPrvFileSource, String prevFileSource,
			ArrayList<Integer> lstIdxOfDeletedLines) {
		ArrayList<Integer> lineIndices = new ArrayList<Integer>();
		
		EditList editList = Utils.getEditListFromDiff(origPrvFileSource, prevFileSource);

		for(Integer idxOfDeletedLine:lstIdxOfDeletedLines){
			
			String[] arrOrigPrvFileSource = origPrvFileSource.split("\n");
			String[] arrPrevFileSource = prevFileSource.split("\n");
			
			if(arrPrevFileSource[idxOfDeletedLine].trim().equals(""))
				continue;
			
			Edit prevEdit = null;
			for(Edit edit:editList){
				
				if(edit.getEndB() <= idxOfDeletedLine){
					prevEdit = edit;
					continue;
				}
				
				int endA = prevEdit.getEndA();
				int endB = prevEdit.getEndB();
				int gap = idxOfDeletedLine - endB;
				lineIndices.add(endA + gap);
				if(!arrOrigPrvFileSource[endA + gap].trim().equals(arrPrevFileSource[idxOfDeletedLine].trim())){
					System.err.println("Error: line contents are not same in original file source and the file source w/o comments.");
					System.exit(0);
				}
				
				break;
			}
		}
		
		return lineIndices;
	}

	/**
	 * Get all deleted lines in commits. The return object, HashMap, is used to identify a BI commit that induce bug-fixing by a deleted line in the BI commit
	 * @param commits
	 * @return HashMap<String, ArrayList<DeletedLineInCommits>>
	 */
	private HashMap<String, ArrayList<DeletedLineInCommits>> getDeletedLinesInCommits(ArrayList<RevCommit> commits) {

		// deletedLines are order by commit date (DESC, i.e., recent commit first)
		HashMap<String, ArrayList<DeletedLineInCommits>> deletedLines = new HashMap<String, ArrayList<DeletedLineInCommits>>();

		// Traverse all commits to collect deleted lines.
		for(RevCommit rev:commits){
			
			// Get basic commit info
			String sha1 =  rev.name() + "";
			String date = Utils.getStringDateTimeFromCommitTime(rev.getCommitTime());
			
			// Get diffs from affected files in the commit
			RevCommit preRev = rev.getParent(0);
			DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
			df.setRepository(repo);
			df.setDiffComparator(RawTextComparator.DEFAULT);
			df.setDetectRenames(true);
			List<DiffEntry> diffs;
			try {
				// Deal with diff and get only deleted lines
				diffs = df.scan(preRev.getTree(), rev.getTree());
				for (DiffEntry diff : diffs) {
					String oldPath = diff.getOldPath();
					String newPath = diff.getNewPath();

					// Skip test case files
					if(newPath.indexOf("Test")>=0 || !newPath.endsWith(".java")) continue;

					// Do diff on files without comments to only consider code lines
					String prevfileSource=Utils.removeLineComments(Utils.fetchBlob(repo, sha1 +  "~1", oldPath));
					String fileSource=Utils.removeLineComments(Utils.fetchBlob(repo, sha1, newPath));	
					EditList editList = Utils.getEditListFromDiff(prevfileSource, fileSource);
					String[] arrPrevfileSource=Utils.removeLineComments(Utils.fetchBlob(repo, sha1 +  "~1", oldPath)).split("\n");
					for(Edit edit:editList){
						// Deleted lines are in DELETE and REPLACE types. So, ignore INSERT type.
						if(!edit.getType().equals(Edit.Type.INSERT)){

							int beginA = edit.getBeginA();
							int endA = edit.getEndA();

							// TODO compute actual line num in the original source file with comments
							for(int lineIdx = beginA; lineIdx < endA; lineIdx++){
								String line = arrPrevfileSource[lineIdx].trim();
								if(line.length() <2) continue; // heuristic: ignore "}" or "{". only consider the line whose length >= 2
								DeletedLineInCommits deletedLine = new DeletedLineInCommits(sha1,date,oldPath,newPath,lineIdx+1,line);
								if(!deletedLines.containsKey(line)){
									ArrayList<DeletedLineInCommits> lstDeletedLines = new ArrayList<DeletedLineInCommits>();
									lstDeletedLines.add(deletedLine);
									deletedLines.put(line,lstDeletedLines);
								}else{
									deletedLines.get(line).add(deletedLine);
								}
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			df.close();
		}

		return deletedLines;
	}

	private ArrayList<RevCommit> getRevCommits() {
		ArrayList<RevCommit> commits = new ArrayList<RevCommit>();

		try {

			git = Git.open( new File(gitURI) );

			Iterable<RevCommit> logs = git.log()
					.call();

			//SimpleDateFormat ft =  new SimpleDateFormat ("E yyyy.MM.dd 'at' HH:mm:ss zzz");
			for (RevCommit rev : logs) {
				Date commitDate = new Date(rev.getCommitTime()* 1000L);

				if(startDate.compareTo(commitDate)<=0 && commitDate.compareTo(endDate)<=0)
					commits.add(rev);
			}             

		} catch (IOException | GitAPIException e) {
			System.err.println("Repository does not exist: " + gitURI);
		}

		return commits;
	}

	Options createOptions(){

		// create Options object
		Options options = new Options();

		// add options
		options.addOption(Option.builder("g").longOpt("git")
				.desc("Git URI")
				.hasArg()
				.argName("URI")
				.required()
				.build());

		options.addOption(Option.builder("b").longOpt("bugs")
				.desc("A file path for bug report IDs")
				.hasArg()
				.argName("file")
				.build());

		options.addOption(Option.builder("s").longOpt("startdate")
				.desc("Start date for collecting bug-introducing changes")
				.hasArg()
				.argName("Start date")
				.required()
				.build());

		options.addOption(Option.builder("e").longOpt("enddate")
				.desc("End date for collecting bug-introducing changes")
				.hasArg()
				.argName("End date")
				.required()
				.build());

		options.addOption(Option.builder("h").longOpt("help")
				.desc("Help")
				.build());


		options.addOption(Option.builder("v").longOpt("verbose")
				.desc("Verbose")
				.build());

		return options;
	}

	boolean parseOptions(Options options,String[] args){

		CommandLineParser parser = new DefaultParser();

		try {

			CommandLine cmd = parser.parse(options, args);

			gitURI = cmd.getOptionValue("g");
			pathToBuggyIDs = cmd.getOptionValue("b");

			help = cmd.hasOption("h");
			//verbose = cmd.hasOption("v");

			startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(cmd.getOptionValue("s"));
			endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(cmd.getOptionValue("e"));

		} catch (Exception e) {
			printHelp(options);
			return false;
		}

		return true;
	}

	private void printHelp(Options options) {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		String header = "Execute BICER to collect bug-introducing changes. On Windows, use BICER.bat instead of ./BICER";
		String footer ="\nPlease report issues at https://github.com/lifove/BICER/issues";
		formatter.printHelp( "./BICER", header, options, footer, true);
	}
}
