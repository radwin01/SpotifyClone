package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;
import com.sun.net.httpserver.HttpExchange;
import org.neo4j.driver.v1.Transaction;

import static org.neo4j.driver.v1.Values.parameters;


@Repository
public class ProfileDriverImpl implements ProfileDriver {

  Driver driver = ProfileMicroserviceApplication.driver;

  public static void InitProfileDb() {
    String queryStr;

    try (Session session = ProfileMicroserviceApplication.driver.session()) {
      try (Transaction trans = session.beginTransaction()) {
        queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
        trans.run(queryStr);

        queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
        trans.run(queryStr);

        queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
        trans.run(queryStr);

        trans.success();
      }
      session.close();
    }
  }


  @Override
  public DbQueryStatus createUserProfile(String userName, String fullName, String password) {

    if (userName != "" && fullName != "" && password != "") {

      try (Session session = ProfileMicroserviceApplication.driver.session()) {

        try (Transaction trans = session.beginTransaction()) {

          Map<String, Object> params = new HashMap<String, Object>();
          params.put("username", userName);
          params.put("fullname", fullName);
          params.put("password", password);
          params.put("playlistName", userName + "-favorites");


          // check if a profile with username userName already exists
          String query1 = "MATCH (p:profile) WHERE p.userName = $username return p";
          StatementResult res1 = trans.run(query1, params);
          if (res1.hasNext()) {
            return new DbQueryStatus("A profile with that username already exists.",
                DbQueryExecResult.QUERY_ERROR_GENERIC);
          }

          String query2 =
              "CREATE (p:profile {userName: $username, fullName: $fullname, password: $password})";
          trans.run(query2, params);
          String query3 = "MERGE (p:playlist {plName: $playlistName})";
          trans.run(query3, params);
          String query4 =
              "MATCH (p:profile), (pl:playlist) WHERE p.userName = $username AND pl.plName = $playlistName CREATE (p)-[:created]->(pl)";
          trans.run(query4, params);

          trans.success();
          session.close();
          return new DbQueryStatus("Profile successfully created!", DbQueryExecResult.QUERY_OK);

        } catch (Exception e) {
          return new DbQueryStatus("Oh no! Something went wrong in creating your profile.",
              DbQueryExecResult.QUERY_ERROR_GENERIC);
        }

      } catch (Exception e) {
        return new DbQueryStatus("Oops! Something went wrong in creating your profile.",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
      }

    } else {
      return new DbQueryStatus("Could not create profile! Make sure all parameters are filled.",
          DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }


  @Override
  public DbQueryStatus followFriend(String userName, String frndUserName) {

    if (userName != "" && frndUserName != "") {

      try (Session session = ProfileMicroserviceApplication.driver.session()) {

        try (Transaction trans = session.beginTransaction()) {

          Map<String, Object> params = new HashMap<String, Object>();
          params.put("username", userName);
          params.put("friendUsername", frndUserName);

          // check if both users exist in the db

          String query1 = "MATCH (p:profile) WHERE p.userName = $username return p";
          StatementResult res1 = trans.run(query1, params);

          String query2 = "MATCH (p:profile) WHERE p.userName = $friendUsername return p";
          StatementResult res2 = trans.run(query2, params);

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
  public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
    if (userName != "" && frndUserName != "") {

      try (Session session = ProfileMicroserviceApplication.driver.session()) {

        try (Transaction trans = session.beginTransaction()) {

          Map<String, Object> params = new HashMap<String, Object>();
          params.put("username", userName);
          params.put("friendUsername", frndUserName);

          // check if both users exist in the db

          String query1 = "MATCH (p:profile) WHERE p.userName = $username return p";
          StatementResult res1 = trans.run(query1, params);

          String query2 = "MATCH (p:profile) WHERE p.userName = $friendUsername return p";
          StatementResult res2 = trans.run(query2, params);

          if (res1.hasNext() && res2.hasNext()) {
            
            // check if a connection between the two profiles already exists
            String query =
                "MATCH (p:profile), (fp:profile), ((p)-[r:follows]->(fp)) WHERE p.userName = $username AND fp.userName = $friendUsername RETURN r";
            StatementResult res = trans.run(query, params);

            if (!res.hasNext()) {
              return new DbQueryStatus("Cannot unfollow a user you haven't followed!",
                  DbQueryExecResult.QUERY_ERROR_GENERIC);
            }

            String realQuery =
                "MATCH (p:profile), (fp:profile), ((p)-[r:follows]->(fp)) WHERE p.userName = $username AND fp.userName = $friendUsername DELETE r";
            trans.run(realQuery, params);
          } else {
            return new DbQueryStatus(
                "Could not unfollow. Make sure both usernames are valid!",
                DbQueryExecResult.QUERY_ERROR_GENERIC);
          }

          trans.success();
          session.close();
          return new DbQueryStatus("You have unfollowed this user.", DbQueryExecResult.QUERY_OK);

        } catch (Exception e) {
          return new DbQueryStatus("Oh no! Something went wrong in unfollowing this user.",
              DbQueryExecResult.QUERY_ERROR_GENERIC);
        }

      } catch (Exception e) {
        return new DbQueryStatus("Oops! omething went wrong in unfollowing this user.",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
      }

    } else {
      return new DbQueryStatus("Could not unfollow! Make sure all parameters are filled.",
          DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

  @Override
  public DbQueryStatus getAllSongFriendsLike(String userName) {

    if (userName != "") {

      try (Session session = ProfileMicroserviceApplication.driver.session()) {

        try (Transaction trans = session.beginTransaction()) {

          Map<String, Object> params = new HashMap<String, Object>();
          params.put("username", userName);
       
          // check if user exist in the db

          String firstQuery = "MATCH (p:profile) WHERE p.userName = $username return p";
          StatementResult res1 = trans.run(firstQuery, params);

          if (res1.hasNext()) {
            
            // check if a connection between the two profiles already exists
            String query =
                "MATCH (p:profile), (fp:profile), ((p)-[r:follows]->(fp)) WHERE p.userName = $username RETURN fp";
            StatementResult res = trans.run(query, params);

            while (res.hasNext()) {
              
              String ret = "";
              ret += res.toString();
              
              return new DbQueryStatus(ret,
                  DbQueryExecResult.QUERY_ERROR_GENERIC);
            }
            
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
}
