package ast_utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

class ASTParser {

	private static final String SPLIT_BY = ",";
	private static final String DEFAULT_FILE_LOC = "programs_augmented.json";
	
	private static final String treeIdx = "program_id";
	private static final String value = "value";
	private static final String nodes = "nodes";
	private static final String nodeIdx = "id";
	private static final String parentIdx = "parent";
	private static final String prevNodeValueIdx = "previous_id";
	private static final String childrenIdx = "children";
	private static final String type = "type";
	
	
	HashMap<Integer, ASTNode> parse(String fileLoc, int goalTreeIdx) {
		if (fileLoc.equals("")) {
			fileLoc = DEFAULT_FILE_LOC;
		}

		HashMap<Integer, ASTNode> ASTStore = new HashMap<Integer, ASTNode>();
		BufferedReader jsonReader = null;
		try {
			jsonReader = new BufferedReader(new FileReader(fileLoc));
		    
			String jsonLine = jsonReader.readLine();
			if (!jsonLine.equals("")) {
				// if JSON file wasn't empty, parse data
				JsonParser parser = new JsonParser();

				
				JsonElement jElement = parser.parse(jsonLine);
				JsonObject jObj = jElement.getAsJsonObject();
				JsonObject jTreeIdx = jObj.getAsJsonObject(treeIdx);
				int currTreeIdx = jTreeIdx.get(value).getAsInt();
				
				if (currTreeIdx != goalTreeIdx)
					return null;
				
				JsonArray mainArray = jObj.getAsJsonArray(nodes);
				for (int i = 0; i < mainArray.size(); i++) {
					JsonObject ndObject = mainArray.get(i).getAsJsonObject();
					
					Integer ndIdx = ndObject.get(nodeIdx).getAsInt();
					
					String ndParentIdxStr = ndObject.get(parentIdx).getAsString();
					Integer ndParentIdx = ndParentIdxStr.equals("") ? -1 : Integer.parseInt(ndParentIdxStr);
					
					String ndPrevValueIdxStr = ndObject.get(prevNodeValueIdx).getAsString();
					Integer ndPrevValueIdx = ndPrevValueIdxStr.equals("") ? -1 : Integer.parseInt(ndPrevValueIdxStr);
					
					String ndValue = ndObject.get(value).getAsString();
					
					String ndType = ndObject.get(type).getAsString();
					
					String ndChildrenStr = ndObject.get(childrenIdx).getAsString();
					Integer[] ndChildren = null;
					if (!ndChildrenStr.equals("")) {
						String[] ndChildrenStrSplit = ndChildrenStr.substring(1, ndChildrenStr.length()-1).replaceAll("\\s+","").split(",");
						ndChildren = new Integer[ndChildrenStrSplit.length];	
						for (int j = 0; j < ndChildrenStrSplit.length; j++) {
							ndChildren[j] = Integer.parseInt(ndChildrenStrSplit[j]);
						}	
					} else {
						ndChildren = new Integer[0];
					}
					
					ASTStore.put(ndIdx, new ASTNode(ndIdx, ndParentIdx, ndPrevValueIdx, ndChildren, ndType, ndValue));
				}
				//int currTreeIdx = mainArray.getAsInt(treeIdx);
				
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (jsonReader != null) {
				try {
					jsonReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return ASTStore;
		
		
	}
	
	private HashMap<Integer, ASTNode> parseCSV(String fileLoc) {
		if (fileLoc.equals("")) {
			fileLoc = DEFAULT_FILE_LOC;
		}

		HashMap<Integer, ASTNode> ASTStore = new HashMap<Integer, ASTNode>();
		BufferedReader csvReader = null;
		try {
			csvReader = new BufferedReader(new FileReader(fileLoc));
			
			// parse one ASTNode per line of CSV data
			String line = "";
			while ((line = csvReader.readLine()) != null) {
				String[] ndData = line.split(SPLIT_BY);
				
				if (ndData[0].equals("") || ndData[1].equals("") || ndData[3].equals(""))
					throw new MalformedCsvException();
				
				// parse atomic data
				Integer ndIdx = Integer.parseInt(ndData[0]);
				Integer ndParentIdx = Integer.parseInt(ndData[1]);
				String ndType = ndData[3];
				
				// parse array data (indices of children of the current AST Node)
				Integer[] ndChildrenIdx;
				if (ndData[2].equals("")) {
					ndChildrenIdx = new Integer[0];
				} else {
					String[] ndChildren = ndData[2].substring(1,ndData[2].length()-1).split("_");
					int ndChildrenCnt = ndChildren.length;
					
					ndChildrenIdx = new Integer[ndChildrenCnt];
					if (ndChildrenCnt != 0) {
						for (int i = 0; i < ndChildrenCnt; i++) {
							ndChildrenIdx[i] = Integer.parseInt(ndChildren[i]);
						}
					}
				}
				ASTStore.put(ndIdx, new ASTNode(ndIdx, ndParentIdx, 0, ndChildrenIdx, ndType, ""));
				
			}
			
		} catch (MalformedCsvException e) {
			e.printStackTrace();
			System.out.println("ASTParser: CSV file with converted AST in " + fileLoc + " is malformed");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("ASTParser: Couldn't find file: " + fileLoc);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("ASTParser: IO exception reading line from file: " + fileLoc);
		} finally {
			if (csvReader != null) {
				try {
					csvReader.close();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("ASTParser: Failed to close BufferedReader from file: " + fileLoc);
				}
			}
		}

		return ASTStore;
	}
}