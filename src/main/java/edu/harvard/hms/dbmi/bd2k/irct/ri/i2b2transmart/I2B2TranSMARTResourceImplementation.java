/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package edu.harvard.hms.dbmi.bd2k.irct.ri.i2b2transmart;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import edu.harvard.hms.dbmi.bd2k.irct.exception.ResourceInterfaceException;
import edu.harvard.hms.dbmi.bd2k.irct.model.find.FindByPath;
import edu.harvard.hms.dbmi.bd2k.irct.model.find.FindInformationInterface;
import edu.harvard.hms.dbmi.bd2k.irct.model.ontology.Entity;
import edu.harvard.hms.dbmi.bd2k.irct.model.ontology.OntologyRelationship;
import edu.harvard.hms.dbmi.bd2k.irct.model.query.Query;
import edu.harvard.hms.dbmi.bd2k.irct.model.query.SelectClause;
import edu.harvard.hms.dbmi.bd2k.irct.model.resource.PrimitiveDataType;
import edu.harvard.hms.dbmi.bd2k.irct.model.resource.ResourceState;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.Result;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.ResultDataType;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.ResultStatus;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.exception.PersistableException;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.exception.ResultSetException;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.tabular.Column;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.tabular.ResultSet;
import edu.harvard.hms.dbmi.bd2k.irct.model.security.SecureSession;
import edu.harvard.hms.dbmi.bd2k.irct.ri.i2b2.I2B2XMLResourceImplementation;

/**
 * An implementation of a resource that communicates with the tranSMART
 * instance. It extends the i2b2 XML resource implementation.
 * 
 */
public class I2B2TranSMARTResourceImplementation extends
		I2B2XMLResourceImplementation {
	private String transmartURL;

	@Override
	public void setup(Map<String, String> parameters)
			throws ResourceInterfaceException {
		String[] strArray = { "resourceName", "resourceURL", "transmartURL",
				"domain" };
		if (!parameters.keySet().containsAll(Arrays.asList(strArray))) {
			throw new ResourceInterfaceException("Missing parameters");
		}

		this.transmartURL = parameters.get("transmartURL");

		super.setup(parameters);
	}

	@Override
	public List<Entity> getPathRelationship(Entity path,
			OntologyRelationship relationship, SecureSession session)
			throws ResourceInterfaceException {
		List<Entity> returns = super.getPathRelationship(path, relationship,
				session);

		// Get the counts from the tranSMART server
		try {
			HttpClient client = createClient(session);
			String basePath = path.getPui();
			String[] pathComponents = basePath.split("/");

			if (pathComponents.length > 3) {
				String myPath = "\\";
				for (String pathComponent : Arrays.copyOfRange(pathComponents,
						3, pathComponents.length)) {
					myPath += "\\" + pathComponent;
				}
				basePath = pathComponents[0] + "/" + pathComponents[1] + "/"
						+ pathComponents[2];

				HttpPost post = new HttpPost(this.transmartURL
						+ "/chart/childConceptPatientCounts");
				List<NameValuePair> formParameters = new ArrayList<NameValuePair>();
				formParameters.add(new BasicNameValuePair("charttype",
						"childconceptpatientcounts"));
				formParameters.add(new BasicNameValuePair("concept_key", myPath
						+ "\\"));
				formParameters.add(new BasicNameValuePair("concept_level", ""));

				post.setEntity(new UrlEncodedFormEntity(formParameters));

				HttpResponse response = client.execute(post);

				JsonReader jsonReader = Json.createReader(response.getEntity()
						.getContent());

				JsonObject counts = jsonReader.readObject().getJsonObject(
						"counts");

				for (Entity singleReturn : returns) {
					String singleReturnMyPath = convertPUItoI2B2Path(singleReturn
							.getPui());

					if (counts.containsKey(singleReturnMyPath)) {
						singleReturn.getCounts().put("count",
								counts.getInt(singleReturnMyPath));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return returns;
	}

	@Override
	public Result runQuery(SecureSession session, Query query, Result result)
			throws ResourceInterfaceException {
		result = super.runQuery(session, query, result);

		if (result.getResultStatus() != ResultStatus.ERROR) {
			String resultInstanceId = result.getResourceActionId();
			String resultId = resultInstanceId.split("\\|")[2];
			try {
				// Wait for it to be either ready or fail
				result = checkForResult(session, result);
				while ((result.getResultStatus() != ResultStatus.ERROR)
						&& (result.getResultStatus() != ResultStatus.COMPLETE)) {
					Thread.sleep(3000);
					result = checkForResult(session, result);
				}
				if (result.getResultStatus() == ResultStatus.ERROR) {
					return result;
				}
				result.setResultStatus(ResultStatus.RUNNING);

				String queryType = null;

				// Gather Select Clauses
				String gatherAllEncounterFacts = "false";
				Map<String, String> aliasMap = new HashMap<String, String>();

				for (SelectClause selectClause : query
						.getClausesOfType(SelectClause.class)) {
					String pui = selectClause.getParameter().getPui()
							.replaceAll("/" + this.resourceName + "/", "");

					if (pui.contains("/")) {
						if ((queryType != null)
								&& (!queryType.equals("CLINICAL"))) {
							result.setResultStatus(ResultStatus.ERROR);
							result.setMessage("Error in select parameters: To many select parameters, or mixed select parameter types");
							return result;
						}
						queryType = "CLINICAL";

						pui = convertPUItoI2B2Path(selectClause.getParameter()
								.getPui());
					}

					aliasMap.put(pui, selectClause.getAlias());
				}

				// Run Additional Queries and Create Result Set
				if (queryType == null) {
					result.setResultStatus(ResultStatus.ERROR);
					result.setMessage("Unknown queryType");
					return result;
				}

				switch (queryType) {
				case "CLINICAL":
					result = runClinicalDataQuery(session, result, aliasMap,
							gatherAllEncounterFacts, resultId);
					result.setResultStatus(ResultStatus.COMPLETE);
					break;
				default:
					result.setResultStatus(ResultStatus.ERROR);
					result.setMessage("Unknown queryType");
				}

				// Set the status to complete
			} catch (InterruptedException | UnsupportedOperationException
					| IOException | ResultSetException | PersistableException e) {
				result.setResultStatus(ResultStatus.ERROR);
				result.setMessage(e.getMessage());
			}
		}
		return result;
	}

	private Result runClinicalDataQuery(SecureSession session, Result result,
			Map<String, String> aliasMap, String gatherAllEncounterFacts,
			String resultId) throws ResultSetException,
			ClientProtocolException, IOException, PersistableException {
		ResultSet rs = (ResultSet) result.getData();
		if (rs.getSize() == 0) {
			rs = createInitialDataset(result, aliasMap, gatherAllEncounterFacts);
			result.setData(rs);
		}

		// Loop through the columns submitting and appending to the
		// rows every 10
		List<String> parameterList = new ArrayList<String>();
		int counter = 0;
		String parameters = "";
		for (String param : aliasMap.keySet()) {
			if (counter == 10) {
				parameterList.add(parameters);
				counter = 0;
				parameters = "";
			}
			if (!parameters.equals("")) {
				parameters += "|";
			}
			parameters += param;
		}
		if (!parameters.equals("")) {
			parameterList.add(parameters);
		}

		for (String parameter : parameterList) {
			// Call the tranSMART API to get the dataset
			String url = this.transmartURL
					+ "/ClinicalData/retrieveClinicalData?rid=" + resultId
					+ "&conceptPaths=" + URLEncoder.encode(URLDecoder.decode(parameter, "UTF-8"), "UTF-8")
					+ "&gatherAllEncounterFacts=" + gatherAllEncounterFacts;
			HttpClient client = createClient(session);
			HttpGet get = new HttpGet(url);
			HttpResponse response = client.execute(get);
			JsonReader reader = Json.createReader(response.getEntity()
					.getContent());
			
			JsonArray arrayResults = reader.readArray();
			// Convert the dataset to Tabular format
			result = convertJsonToResultSet(result, arrayResults, aliasMap,
					gatherAllEncounterFacts);
		}

		return result;
	}

	private ResultSet createInitialDataset(Result result,
			Map<String, String> aliasMap, String gatherAllEncounterFacts)
			throws ResultSetException {
		ResultSet rs = (ResultSet) result.getData();

		// Set up the columns
		Column idColumn = new Column();
		idColumn.setName("PATIENT_NUM");
		idColumn.setDataType(PrimitiveDataType.STRING);
		rs.appendColumn(idColumn);

		if (gatherAllEncounterFacts.equalsIgnoreCase("true")) {
			Column encounterColumn = new Column();
			encounterColumn.setName("ENCOUNTER_NUM");
			encounterColumn.setDataType(PrimitiveDataType.STRING);
			rs.appendColumn(encounterColumn);
		}

		for (String aliasKey : aliasMap.keySet()) {
			Column newColumn = new Column();
			if (aliasMap.get(aliasKey) == null) {
				newColumn.setName(aliasKey);
			} else {
				newColumn.setName(aliasMap.get(aliasKey));
			}
			newColumn.setDataType(PrimitiveDataType.STRING);

			rs.appendColumn(newColumn);
		}
		result.setData(rs);
		return rs;
	}

	private Result convertJsonToResultSet(Result result,
			JsonArray arrayResults, Map<String, String> aliasMap,
			String gatherAllEncounterFacts) throws ResultSetException,
			PersistableException {
		// If the resultset is empty create the initial result set
		ResultSet rs = (ResultSet) result.getData();

		// Get additional fields to grab from alias
		List<String> additionalFields = new ArrayList<String>();
		for (String key : aliasMap.keySet()) {
			if (!key.startsWith("\\")) {
				additionalFields.add(key);
			}
		}

		// Create the initial Matrix
		Map<String, Map<String, String>> dataMatrix = new HashMap<String, Map<String, String>>();

		String pivot = "PATIENT_NUM";
		if (gatherAllEncounterFacts.equalsIgnoreCase("true")) {
			pivot = "ENCOUNTER_NUM";
			additionalFields.add("PATIENT_NUM");
		}

		for (JsonValue val : arrayResults) {
			JsonObject obj = (JsonObject) val;
			String rowId = obj.getString(pivot);

			if (!dataMatrix.containsKey(rowId)) {
				dataMatrix.put(rowId, new HashMap<String, String>());
			}

			dataMatrix.get(rowId).put(obj.getString("CONCEPT_PATH"),
					obj.getString("VALUE"));

			for (String field : additionalFields) {
				if (obj.containsKey(field)) {
					dataMatrix.get(rowId).put(field, obj.getString(field));
				}
			}
			// if (gatherAllEncounterFacts.equalsIgnoreCase("true")) {
			// dataMatrix.get(rowId).put("PATIENT_NUM",
			// obj.getString("PATIENT_NUM"));
			// }
		}

		// Loop through the result set and add the information in the matrix to
		// the result set
		rs.first();
		while (rs.next()) {
			String rsRowId = rs.getString(pivot);
			if (dataMatrix.containsKey(rsRowId)) {
				Map<String, String> newRowData = dataMatrix.get(rsRowId);
				for (String colKeySet : newRowData.keySet()) {
					// Check to see if an alias exists
					if (aliasMap.get(colKeySet) != null) {
						rs.updateString(aliasMap.get(colKeySet),
								newRowData.get(colKeySet));
					} else {
						rs.updateString(colKeySet, newRowData.get(colKeySet));
					}
				}
				dataMatrix.remove(rsRowId);
			}
		}
		// If the information is still in the matrix add it to the result set at
		// the end
		for (String rowId : dataMatrix.keySet()) {
			rs.appendRow();
			rs.updateString(pivot, rowId);

			Map<String, String> newRowData = dataMatrix.get(rowId);
			for (String colKeySet : newRowData.keySet()) {
				// Check to see if an alias exists
				if (aliasMap.get(colKeySet) != null) {
					rs.updateString(aliasMap.get(colKeySet),
							newRowData.get(colKeySet));
				} else {
					rs.updateString(colKeySet, newRowData.get(colKeySet));
				}
			}
		}

		// Add results back
		result.setData(rs);
		return result;
	}

	@Override
	public Result getResults(SecureSession session, Result result)
			throws ResourceInterfaceException {
		// This method only exists so the results for i2b2XML do not get called
		return result;
	}

	@Override
	public ResourceState getState() {
		return resourceState;
	}

	@Override
	public ResultDataType getQueryDataType(Query query) {
		return ResultDataType.TABULAR;
	}

	@Override
	public String getType() {
		return "i2b2/tranSMART";
	}

	@Override
	public List<Entity> find(Entity path,
			FindInformationInterface findInformation, SecureSession session)
			throws ResourceInterfaceException {
		List<Entity> returns = new ArrayList<Entity>();

		if (findInformation instanceof FindByPath) {
			FindByPath findPath = (FindByPath) findInformation;
			if (findInformation.getValues().containsKey("tmObservationOnly")) {
				returns = searchObservationOnly(findPath.getValues()
						.get("term"), findPath.getValues().get("strategy"),
						session, findPath.getValues().get("tmObservationOnly"));
			} else {
				returns = searchObservationOnly(findPath.getValues()
						.get("term"), findPath.getValues().get("strategy"),
						session, "FALSE");
			}
		} else {
			returns = super.find(path, findInformation, session);
		}
		return returns;
	}

	public List<Entity> searchObservationOnly(String searchTerm,
			String strategy, SecureSession session, String onlObs) {
		List<Entity> entities = new ArrayList<Entity>();

		try {
			URI uri = new URI(this.transmartURL.split("://")[0],
					this.transmartURL.split("://")[1].split("/")[0], "/"
							+ this.transmartURL.split("://")[1].split("/")[1]
							+ "/textSearch/findPaths", "oblyObs=" + onlObs
							+ "&term=" + searchTerm, null);

			HttpClient client = createClient(session);
			HttpGet get = new HttpGet(uri);
			HttpResponse response = client.execute(get);
			JsonReader reader = Json.createReader(response.getEntity()
					.getContent());
			JsonArray arrayResults = reader.readArray();

			for (JsonValue val : arrayResults) {
				JsonObject returnObject = (JsonObject) val;

				Entity returnedEntity = new Entity();
				returnedEntity
						.setPui("/"
								+ this.resourceName
								+ converti2b2Path(returnObject
										.getString("conceptPath")));

				if (!returnObject.isNull("text")) {
					returnedEntity.getAttributes().put("text",
							returnObject.getString("text"));
				}
				entities.add(returnedEntity);
			}

		} catch (URISyntaxException | JsonException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return entities;
	}

	private String convertPUItoI2B2Path(String pui) {
		String[] singleReturnPathComponents = pui.split("/");
		String singleReturnMyPath = "";
		for (String pathComponent : Arrays.copyOfRange(
				singleReturnPathComponents, 4,
				singleReturnPathComponents.length)) {
			singleReturnMyPath += "\\" + pathComponent;
		}

		singleReturnMyPath += "\\";

		return singleReturnMyPath;
	}
}
