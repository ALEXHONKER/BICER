package ca.uwaterloo.ece.bicer;
import org.junit.Test;

import ca.uwaterloo.ece.bicer.utils.Labeler;

import static org.junit.Assert.*;

/*
 * This Java source file was auto generated by running 'gradle init --type java-library'
 * by 'j22nam' at '27/05/16 5:45 PM' with Gradle 2.13",
 *
 * @author j22nam, @date 27/05/16 5:45 PM
 */
public class LabelerTest {
    @Test public void testSomeLibraryMethod() {
    	
    	String[] luceneTrainingStartDates = {
    			"2010-09-17",
    			"2010-10-16",
    			"2010-11-15",
    			"2010-12-15",
    			"2011-01-15",
    			"2011-02-13",
    			"2011-03-16",
    			"2011-04-15"};
    	
    	String[] luceneTrainingEndDates = {
    			"2010-10-23",
    			"2010-11-21",
    			"2010-12-22",
    			"2011-01-21",
    			"2011-02-19",
    			"2011-03-22",
    			"2011-04-20",
    			"2011-05-20"
    	};
    	
    	String[] luceneTestStartDates = {
    			"2010-11-28",
				"2010-12-29",
				"2011-01-27",
				"2011-02-26",
				"2011-03-29",
				"2011-04-27",
				"2011-05-27",
				"2011-06-27"
    	};
    	
    	String[] luceneTestEndDates = {
    			"2010-12-28",
    			"2011-01-27",
    			"2011-02-26",
    			"2011-03-28",
    			"2011-04-27",
    			"2011-05-27",
    			"2011-06-26",
    			"2011-06-29"
    	};
    	
    	String luceneEndDateForTestLabelCollection = "2014-01-16";
    	
    	String path = System.getProperty("user.home") + "/Documents/ODP/projects/lucene/";
    	
    	String pathToArff = path + "arffsOriginalWOTestCases/0/train.arff";
    	String classAttributeName = "500_Buggy?";
    	String positiveLabel = "1";
    	String pathToChangeIDSha1Pair = path + "change_id_sha1_thin_lucene.txt";
    	String pathToBIChangesForLabeling = path + "biChanges.txt";
    	String pathToBIChangesForOldPaths = path + "biChanges.txt";
    	String pathToNewArff = path + "test.arff";
    	String startDate="2010-09-17";
    	String endDate="2010-10-23 23:59:59";
    	String lastDateForFixCollection="2010-11-29";
    	
    	// lucene training set
    	boolean manual = false;
    	if(manual){
    		pathToBIChangesForLabeling = path + "luceneBIsManualIssueCorrected.txt";
    	}
    	
    	for(int i=0; i<luceneTrainingStartDates.length;i++){
    		
    		pathToArff = path + "arffsOriginalWOTestCases/" + i +"/train.arff";
    		startDate = luceneTrainingStartDates[i];
    		endDate = luceneTrainingEndDates[i] + "23:59:59";
    		lastDateForFixCollection = luceneTestStartDates[i];
    		if(manual)	
    			pathToNewArff = path + "arffsManyallyCleaned/" + i + "/train.arff";
    		else
    			pathToNewArff = path + "arffsCleanedWOTestCases/" + i + "/train.arff";
    		
    		System.out.println("\n\nData index: " + i);
    		
    		Labeler.relabelArff(pathToArff, classAttributeName, positiveLabel, pathToChangeIDSha1Pair,
    				pathToBIChangesForLabeling, pathToNewArff, startDate, endDate, lastDateForFixCollection,
    				pathToBIChangesForOldPaths);
    	}
    	
    	
    	// Lucene test sets
    	/*lastDateForFixCollection = luceneEndDateForTestLabelCollection;
    	for(int i=0; i<luceneTestStartDates.length;i++){
		
			pathToArff = path + "arffsOriginalWOTestCases/" + i +"/test.arff";
			startDate = luceneTestStartDates[i];
			endDate = luceneTestEndDates[i] + "23:59:59";
			if(manual)	
    			pathToNewArff = path + "arffsManyallyCleaned/" + i + "/test.arff";
    		else
    			pathToNewArff = path + "arffsCleanedWOTestCases/" + i + "/test.arff";
			
			System.out.println("\n\nData index: " + i);
			
			Labeler.relabelArff(pathToArff, classAttributeName, positiveLabel, pathToChangeIDSha1Pair,
					pathToBIChangesForLabeling, pathToNewArff, startDate, endDate, lastDateForFixCollection,
					pathToBIChangesForOldPaths);
    	}*/
    	
    	String[] jackrabbitTrainingStartDates = {
    			"2007-09-12",
    			"2007-11-13",
    			"2008-01-11",
    			"2008-03-10",
    			"2008-05-12",
    			"2008-07-16",
    			"2008-09-11",
    			"2008-11-06",
    			"2009-01-06",
    			"2009-03-10",
    			};
    	
    	String[] jackrabbitTrainingEndDates = {
    			"2007-11-23",
    			"2008-01-22",
    			"2008-03-21",
    			"2008-05-22",
    			"2008-07-21",
    			"2008-09-19",
    			"2008-11-18",
    			"2009-01-15",
    			"2009-03-18",
    			"2009-05-15",
    	};
    	
    	String[] jackrabbitTestStartDates = {
    			"2008-02-06",
    			"2008-04-07",
    			"2008-06-05",
    			"2008-08-04",
    			"2008-10-03",
    			"2008-12-02",
    			"2009-02-03",
    			"2009-04-01",
    			"2009-06-02",
    			"2009-07-30",
    	};
    	
    	String[] jackrabbitTestEndDates = {
    			"2008-04-04",
    			"2008-05-28",
    			"2008-07-31",
    			"2008-10-02",
    			"2008-12-01",
    			"2009-01-30",
    			"2009-03-31",
    			"2009-05-29",
    			"2009-07-29",
    			"2009-09-14",
    	};
    	
    	String jackrabbitEndDateForTestLabelCollection = "2013-01-14";
    	
    	path = System.getProperty("user.home") + "/Documents/ODP/projects/jackrabbit/";
    	pathToBIChangesForLabeling = path + "biChanges.txt";
    	classAttributeName = "500_Buggy?";
    	positiveLabel = "1";
    	pathToChangeIDSha1Pair = path + "change_id_sha1_thin_jackrabbit.txt";
    	
    	//manual = true;
    	
    	if(manual){
    		pathToBIChangesForLabeling = path + "jackrabbitBIsManualIssueCorrected.txt";
    	}
    	pathToBIChangesForOldPaths = path + "biChanges.txt";
    	/*for(int i=0; i<jackrabbitTrainingStartDates.length;i++){
    		
    		pathToArff = path + "arffsOriginalWOTestCases/" + i +"/train.arff";
    		startDate = jackrabbitTrainingStartDates[i];
    		endDate = jackrabbitTrainingEndDates[i] + "23:59:59";
    		lastDateForFixCollection = jackrabbitTestStartDates[i];
    		if(manual)	
    			pathToNewArff = path + "arffsManyallyCleaned/" + i + "/train.arff";
    		else
    			pathToNewArff = path + "arffsCleanedWOTestCases/" + i + "/train.arff";
    		
    		System.out.println("\n\nData index: " + i);
    		
    		Labeler.relabelArff(pathToArff, classAttributeName, positiveLabel, pathToChangeIDSha1Pair,
    				pathToBIChangesForLabeling, pathToNewArff, startDate, endDate, lastDateForFixCollection,
    				pathToBIChangesForOldPaths);
    	}
    	
    	// Jackrabbit test sets
    	lastDateForFixCollection = jackrabbitEndDateForTestLabelCollection;
    	for(int i=0; i<jackrabbitTestStartDates.length;i++){
		
			pathToArff = path + "arffsOriginalWOTestCases/" + i +"/test.arff";
			startDate = jackrabbitTestStartDates[i];
			endDate = jackrabbitTestEndDates[i] + "23:59:59";
			if(manual)	
    			pathToNewArff = path + "arffsManyallyCleaned/" + i + "/test.arff";
    		else
    			pathToNewArff = path + "arffsCleanedWOTestCases/" + i + "/test.arff";
			
			System.out.println("\n\nData index: " + i);
			
			Labeler.relabelArff(pathToArff, classAttributeName, positiveLabel, pathToChangeIDSha1Pair,
					pathToBIChangesForLabeling, pathToNewArff, startDate, endDate, lastDateForFixCollection,
					pathToBIChangesForOldPaths);
    	}*/
    	//assertTrue("someLibraryMethod should return 'true'", classUnderTest.someLibraryMethod());
    }
}
