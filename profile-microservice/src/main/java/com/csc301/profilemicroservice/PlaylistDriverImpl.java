package com.csc301.profilemicroservice;

import java.util.HashMap;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
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

  @Override
  public DbQueryStatus likeSong(String userName, String songId) {

    return null;
  }

  @Override
  public DbQueryStatus unlikeSong(String userName, String songId) {

    return null;
  }

  @Override
  public DbQueryStatus deleteSongFromDb(String songId) {
    try (Session session = driver.session()) {
      boolean found = false;
      try (Transaction tx = session.beginTransaction()) {
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
      return new DbQueryStatus("Song not found", DbQueryExecResult.QUERY_OK);
    } catch (Exception e) {
      return new DbQueryStatus("Delete failed", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

  public DbQueryStatus addSongProfile(String songId) {
    try (Session session = driver.session()) {
      try (Transaction tx = session.beginTransaction()) {
        String line = "MERGE (a:song {songId:$y})";
        HashMap<String, Object> params = new HashMap<>();
        params.put("y", songId);
        tx.run(line, params);
        tx.success();
      }
      session.close();
      return new DbQueryStatus("Add complete", DbQueryExecResult.QUERY_OK);
    } catch (Exception e) {
      return new DbQueryStatus("Add failed", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

}
