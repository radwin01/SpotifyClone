package com.csc301.profilemicroservice;

import java.util.HashMap;
import java.util.Map;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.springframework.stereotype.Repository;

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

  /**
   * Method that allows a user to like a song.
   * 
   * @param userName the username of the profile
   * @param songId the id of the song
   * 
   * @return the status of the query: OK if the user has successfully liked the song, ERROR_GENERIC
   *         otherwise
   */
  @Override
  public DbQueryStatus likeSong(String userName, String songId) {

    if (userName != "" && songId != "") {

      try (Session session = ProfileMicroserviceApplication.driver.session()) {

        try (Transaction trans = session.beginTransaction()) {

          Map<String, Object> params = new HashMap<String, Object>();
          params.put("username", userName);
          params.put("id", songId);
          params.put("playlistName", userName + "-favorites");

          // check if both users exist in the db

          String query1 = "MATCH (p:profile) WHERE p.userName = $username return p";
          StatementResult res1 = trans.run(query1, params);

          String query2 = "MATCH (s:song) WHERE s.songId = $id return s";
          StatementResult res2 = trans.run(query2, params);

          if (res1.hasNext() && res2.hasNext()) {

            // check if a connection between the two profiles already exists
            String query =
                "MATCH (pl:playlist), (s:song), ((pl)-[r:includes]->(s)) WHERE pl.plName = $playlistName AND s.songId = $id RETURN r";
            StatementResult res = trans.run(query, params);

            if (res.hasNext()) {
              return new DbQueryStatus("You already like this song!",
                  DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
            }

            // if the query is correct, like song
            String realQuery =
                "MATCH (pl:playlist), (s:song) WHERE pl.plName = $playlistName AND s.songId = $id CREATE (pl)-[:includes]->(s)";
            trans.run(realQuery, params);
          } else {
            // if either the song or profile do not exist
            return new DbQueryStatus(
                "Could not add song. Make sure the profile and song are both valid!",
                DbQueryExecResult.QUERY_ERROR_GENERIC);
          }

          trans.success();
          session.close();
          return new DbQueryStatus("Song has been successfully liked.", DbQueryExecResult.QUERY_OK);

          // include error messages in the status if anything goes wrong in the process
        } catch (Exception e) {
          return new DbQueryStatus("Oh no! Something went wrong in liking this song.",
              DbQueryExecResult.QUERY_ERROR_GENERIC);
        }

      } catch (Exception e) {
        return new DbQueryStatus("Oops! Something went wrong in liking this song.",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
      }

    } else {
      return new DbQueryStatus("Could not like song! Make sure all parameters are filled!",
          DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

  /**
   * Method that allows a user to unlike a song.
   * 
   * @param userName the username of the profile
   * @param songId the id of the song
   * 
   * @return the status of the query: OK if the user has successfully unliked the song,
   *         ERROR_GENERIC otherwise
   */
  @Override
  public DbQueryStatus unlikeSong(String userName, String songId) {

    if (userName != "" && songId != "") {

      try (Session session = ProfileMicroserviceApplication.driver.session()) {

        try (Transaction trans = session.beginTransaction()) {

          Map<String, Object> params = new HashMap<String, Object>();
          params.put("username", userName);
          params.put("id", songId);
          params.put("playlistName", userName + "-favorites");

          // check if user and song exist in the db

          String query1 = "MATCH (p:profile) WHERE p.userName = $username return p";
          StatementResult res1 = trans.run(query1, params);

          String query2 = "MATCH (s:song) WHERE s.songId = $id return s";
          StatementResult res2 = trans.run(query2, params);

          if (res1.hasNext() && res2.hasNext()) {

            // check if a connection between the user's playlist and the song already does not exist

            String query =
                "MATCH (pl:playlist), (s:song), ((pl)-[r:includes]->(s)) WHERE pl.plName = $playlistName AND s.songId = $id RETURN r";
            StatementResult res = trans.run(query, params);

            if (!res.hasNext()) {
              return new DbQueryStatus("Cannot unlike a song you already do not like!",
                  DbQueryExecResult.QUERY_ERROR_GENERIC);
            }

            String realQuery =
                "MATCH (pl:playlist), (s:song), ((pl)-[r:includes]->(s)) WHERE pl.plName = $playlistName AND s.songId = $id DELETE r";
            trans.run(realQuery, params);
          } else {
            // if either the song or profile do not exist
            return new DbQueryStatus(
                "Could not unlike song. Make sure the profile and song are both valid!",
                DbQueryExecResult.QUERY_ERROR_GENERIC);
          }

          trans.success();
          session.close();
          return new DbQueryStatus("Song has been successfully unliked.",
              DbQueryExecResult.QUERY_OK);

          // include error messages in the status if anything goes wrong in the process
        } catch (Exception e) {
          return new DbQueryStatus("Oh no! Something went wrong in unliking this song.",
              DbQueryExecResult.QUERY_ERROR_GENERIC);
        }

      } catch (Exception e) {
        return new DbQueryStatus("Oops! Something went wrong in unliking this song.",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
      }

    } else {
      return new DbQueryStatus("Could not unlike song! Make sure all parameters are filled.",
          DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

  /**
   * Method that deletes a song from the database.
   * 
   * @param songId the id of the song
   * 
   * @return the status of the query: OK if the song is successfully deleted, ERROR_GENERIC
   *         otherwise
   */
  @Override
  public DbQueryStatus deleteSongFromDb(String songId) {
    try (Session session = driver.session()) {
      boolean found = false;
      try (Transaction tx = session.beginTransaction()) {
        // attempt to delete the song with the specified songId from the db
        String line = "MATCH (a:song {songId:$y})\n DETACH DELETE(a) RETURN(a)";
        HashMap<String, Object> params = new HashMap<>();
        params.put("y", songId);
        if (tx.run(line, params).hasNext()) {
          found = true;
        }

        tx.success();
      }
      session.close();
      if (found) {
        return new DbQueryStatus("Delete complete", DbQueryExecResult.QUERY_OK);
      }
      // if not found, include error message in the status
      return new DbQueryStatus("Song not found", DbQueryExecResult.QUERY_ERROR_GENERIC);

      // if anything goes wrong in the process, include error message in the status
    } catch (Exception e) {
      return new DbQueryStatus("Delete failed", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

  /**
   * Method that adds a song to the database.
   * 
   * @param songId the id of the song
   * 
   * @return the status of the query: OK if the song was successfully added, ERROR_GENERIC otherwise
   */
  @Override
  public DbQueryStatus addSongProfile(String songId) {
    try (Session session = driver.session()) {
      try (Transaction tx = session.beginTransaction()) {
        // attempt to add a song with the specified songId to the db
        String line = "MERGE (a:song {songId:$y})";
        HashMap<String, Object> params = new HashMap<>();
        params.put("y", songId);
        tx.run(line, params);
        tx.success();
      }
      session.close();
      return new DbQueryStatus("Add complete", DbQueryExecResult.QUERY_OK);
    } catch (Exception e) {
      // if anything goes wrong in the process, include error message in status
      return new DbQueryStatus("Add failed", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }
}
