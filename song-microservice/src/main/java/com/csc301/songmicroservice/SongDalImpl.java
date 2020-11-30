package com.csc301.songmicroservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class SongDalImpl implements SongDal {

  private final MongoTemplate db;

  @Autowired
  public SongDalImpl(MongoTemplate mongoTemplate) {
    this.db = mongoTemplate;
  }

  @Override
  public DbQueryStatus addSong(Song songToAdd) {

    DbQueryStatus dataToReturn;

    try {
      db.insert(songToAdd);
      dataToReturn = new DbQueryStatus("Add Successful", DbQueryExecResult.QUERY_OK);
      dataToReturn.setData(songToAdd.getJsonRepresentation());
      return dataToReturn;

    } catch (Exception e) {
      return new DbQueryStatus("Unable to add new song", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

  @Override
  public DbQueryStatus findSongById(String songId) {

    DbQueryStatus dataToReturn;

    try {
      Query query = new Query();
      query.addCriteria(Criteria.where("_id").is(songId));
      Song found = db.findOne(query, Song.class);
      if (found != null) {
        dataToReturn = new DbQueryStatus("Search Successful", DbQueryExecResult.QUERY_OK);
        dataToReturn.setData(found.getJsonRepresentation());
        return dataToReturn;
      }
      return new DbQueryStatus("Song not Found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

    } catch (Exception e) {
      return new DbQueryStatus("Could Not retrieve song", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

  @Override
  public DbQueryStatus getSongTitleById(String songId) {
    DbQueryStatus dataToReturn;

    try {
      Query query = new Query();
      query.addCriteria(Criteria.where("_id").is(songId));
      Song found = db.findOne(query, Song.class);
      if (found != null) {
        dataToReturn = new DbQueryStatus("Search Successful", DbQueryExecResult.QUERY_OK);
        dataToReturn.setData(found.getSongName());
        return dataToReturn;
      }
      return new DbQueryStatus("Song not Found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

    } catch (Exception e) {
      return new DbQueryStatus("Could Not retrieve song", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

  @Override
  public DbQueryStatus deleteSongById(String songId) {
    try {
      Query query = new Query();
      query.addCriteria(Criteria.where("_id").is(songId));
      Song found = db.findAndRemove(query, Song.class);
      if (found != null) {
        return new DbQueryStatus("Delete Successful", DbQueryExecResult.QUERY_OK);
      }
      return new DbQueryStatus("Song not Found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

    } catch (Exception e) {
      return new DbQueryStatus("Could Not delete song", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

  @Override
  public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
    try {
      long toAdd = 1;
      if (shouldDecrement) {
        toAdd = -1;
      }
      Query query = new Query();
      query.addCriteria(Criteria.where("_id").is(songId));
      Song found = db.findOne(query, Song.class);
      if (found != null) {
        toAdd = toAdd + found.getSongAmountFavourites();
        if (toAdd >= 0) {
          found.setSongAmountFavourites(toAdd);
          db.findAndReplace(query, found);
          return new DbQueryStatus("Update Successful", DbQueryExecResult.QUERY_OK);
        }
        return new DbQueryStatus("Cannot have negative favourites",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
      }
      return new DbQueryStatus("Song not Found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

    } catch (Exception e) {
      return new DbQueryStatus("Could Not decrement/increment favourites",
          DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }
}
