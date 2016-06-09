package ca.uwaterloo.ece.bicer.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

public class Utils {
	static public ArrayList<String> getLines(String file,boolean removeHeader){
		ArrayList<String> lines = new ArrayList<String>();
		String thisLine="";
		//Open the file for reading
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((thisLine = br.readLine()) != null) { // while loop begins here
				lines.add(thisLine);
			} // end while 
			br.close();
		} // end try
		catch (IOException e) {
			System.err.println("Error: " + e);
			//System.exit(0);
		}

		if(removeHeader)
			lines.remove(0);

		return lines;
	}

	// TODO
	static public EditList getEditListFromDiff(Git git,String oldSha1, String newSha1, String path){

		Repository repo = git.getRepository();

		ObjectId oldId;
		try {
			oldId = repo.resolve(oldSha1 + "^{tree}");
			ObjectId newId = repo.resolve(newSha1 + "^{tree}");


			ObjectReader reader = repo.newObjectReader();

			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, oldId);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, newId);

			List<DiffEntry> diffs= git.diff()
					.setPathFilter(PathFilter.create(path))
					.setNewTree(newTreeIter)
					.setOldTree(oldTreeIter)
					.call();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DiffFormatter df = new DiffFormatter(out);
			df.setRepository(repo);

			//for(DiffEntry entry : diffs)
			//{
			DiffEntry entry = diffs.get(0);
			df.format(entry);
			String diffText = out.toString("UTF-8");

			System.out.println( diffText );
			FileHeader fileHeader = df.toFileHeader( entry );
			List<? extends HunkHeader> hunks = fileHeader.getHunks();
			EditList editList = fileHeader.toEditList();

			for(Edit edit:editList){
				System.out.println(edit.toString());
			}

			df.close();
			
			return editList;
			//}

		} catch (RevisionSyntaxException | IOException | GitAPIException e) {
			e.printStackTrace();
		}
		return null;
	}

	static public String fetchBlob(Repository repo, String revSpec, String path) throws MissingObjectException, IncorrectObjectTypeException,
	IOException {

		// Resolve the revision specification
		final ObjectId id = repo.resolve(revSpec);

		// Makes it simpler to release the allocated resources in one go
		ObjectReader reader = repo.newObjectReader();

		try {
			// Get the commit object for that revision
			RevWalk walk = new RevWalk(reader);
			RevCommit commit = walk.parseCommit(id);
			walk.close();

			// Get the revision's file tree
			RevTree tree = commit.getTree();
			// .. and narrow it down to the single file's path
			TreeWalk treewalk = TreeWalk.forPath(reader, path, tree);

			if (treewalk != null) {
				// use the blob id to read the file's data
				byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
				return new String(data, "utf-8");
			} else {
				return "";
			}

		} finally {
			reader.close();
		}
	}

	static public boolean doesSameLineExist(String line,String[] lines,boolean trim,boolean ignoreLineComments){

		line = ignoreLineComments?removeLineComments(line):line;

		for(String lineCompare:lines){
			lineCompare = ignoreLineComments?removeLineComments(lineCompare):lineCompare;
			if(trim){
				if(line.trim().equals(lineCompare.trim()))
					return true;
			}
			else{
				if(line.equals(lineCompare))
					return true;
			}
		}

		return false;
	}

	public static String removeLineComments(String line) {
		// http://stackoverflow.com/questions/2613432/remove-source-file-comments-using-intellij
		return line.replaceAll("(/\\*([^*]|[\\r\\n]|(\\*+([^*/]|[\\r\\n])))*\\*+/|[ \\t]*//.*)", "");
	}

	public static String getStringFromStringArray(String[] wholeFixCode) {

		String string="";

		for(String line:wholeFixCode)
			string += line +"\n";

		return string;
	}
}
