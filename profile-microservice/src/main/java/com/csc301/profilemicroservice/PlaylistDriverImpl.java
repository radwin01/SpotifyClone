package com.csc301.profilemicroservice;

import java.util.HashMap;
import java.util.Map;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {

	  if (userName != "" && songId != "") {

	      try (Session session = ProfileMicroserviceApplication.driver.session()) {

	        try (Transaction trans = session.beginTransaction()) {

	          Map<String, Object> params = new HashMap<String, Object>();
	          params.put("username", userName);
	          params.put("songID", songId);

	          // check if both users exist in the db

	          String firstQuery = "MATCH (p:profile) WHERE p.userName = $username return p";
	          StatementResult res1 = trans.run(firstQuery, params);

	          String secondQuery = "MATCH (p:profile) WHERE p.userName = $friendUsername return p";
	          StatementResult res2 = trans.run(secondQuery, params);

	          if (res1.hasNext() && res2.hasNext()) {
	            
	            // check if a connection between the two profiles already exists
	            String query =
	                "MATCH (p:profile), (fp:profile), ((p)-[r:follows]->(fp)) WHERE p.userName = $username AND fp.userName = $friendUsername RETURN r";
	            StatementResult res = trans.run(query, params);

	            if (res.hasNext()) {
	              return new DbQueryStatus("You already follow this user!",
	                  DbQueryExecResult.QUERY_ERROR_GENERIC);
	            }

	            String realQuery =
	                "MATCH (p:profile), (fp:profile) WHERE p.userName = $username AND fp.userName = $friendUsername CREATE (p)-[:follows]->(fp)";
	            trans.run(realQuery, params);
	          } else {
	            return new DbQueryStatus(
	                "Could not follow. Make sure both usernames are valid!",
	                DbQueryExecResult.QUERY_ERROR_GENERIC);
	          }

	          trans.success();
	          session.close();
	          return new DbQueryStatus("You now follow this user.", DbQueryExecResult.QUERY_OK);

	        } catch (Exception e) {
	          return new DbQueryStatus("Oh no! Something went wrong in following this user.",
	              DbQueryExecResult.QUERY_ERROR_GENERIC);
	        }

	      } catch (Exception e) {
	        return new DbQueryStatus("Oops! omething went wrong in following this user.",
	            DbQueryExecResult.QUERY_ERROR_GENERIC);
	      }

	    } else {
	      return new DbQueryStatus("Could not follow! Make sure all parameters are filled.",
	          DbQueryExecResult.QUERY_ERROR_GENERIC);
	    }
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		
		return null;
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		
		return null;
	}
}
