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

  /**
   * Adds a given song to the database
   * 
   * @param SongToAdd: The song to add to the database
   * @return DbQueryStatus: The result of the search with the returned data and status
   */
  @Override
  public DbQueryStatus addSong(Song songToAdd) {

    // Create new DbQueryStatus
    DbQueryStatus dataToReturn;

    try {

      // Attempt to insert the song and return data
      db.insert(songToAdd);
      dataToReturn = new DbQueryStatus("Add Successful", DbQueryExecResult.QUERY_OK);
      dataToReturn.setData(songToAdd.getJsonRepresentation());
      return dataToReturn;

    } catch (Exception e) {

      // Return error status if failed
      return new DbQueryStatus("Unable to add new song", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

  /**
   * Finds a song in the database by the given ID and returns its information
   * 
   * @param SongId: The id of the song to search for
   * @return DbQueryStatus: The result of the search with the returned data and status
   */
  @Override
  public DbQueryStatus findSongById(String songId) {

    // Create new DbQueryStatus
    DbQueryStatus dataToReturn;

    try {

      // Create new query to search by id
      Query query = new Query();
      query.addCriteria(Criteria.where("_id").is(songId));
      Song found = db.findOne(query, Song.class);

      // If song exists return info
      if (found != null) {
        dataToReturn = new DbQueryStatus("Search Successful", DbQueryExecResult.QUERY_OK);
        dataToReturn.setData(found.getJsonRepresentation());
        return dataToReturn;
      }

      // If song does not exist return not found
      return new DbQueryStatus("Song not Found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

    } catch (Exception e) {
      // Return error status if failed
      return new DbQueryStatus("Could Not retrieve song", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

  /**
   * Finds a song in the database by the given ID and returns its title
   * 
   * @param SongId: The id of the song to search for
   * @return DbQueryStatus: The result of the search with the returned data and status
   */
  @Override
  public DbQueryStatus getSongTitleById(String songId) {

    // Create new DbQueryStatus
    DbQueryStatus dataToReturn;

    try {

      // Create new query to search by id
      Query query = new Query();
      query.addCriteria(Criteria.where("_id").is(songId));
      Song found = db.findOne(query, Song.class);

      // If song exists return info
      if (found != null) {
        dataToReturn = new DbQueryStatus("Search Successful", DbQueryExecResult.QUERY_OK);
        dataToReturn.setData(found.getSongName());
        return dataToReturn;
      }

      // If song does not exist return not found
      return new DbQueryStatus("Song not Found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

    } catch (Exception e) {

      // Return error status if failed
      return new DbQueryStatus("Could Not retrieve song", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

  /**
   * Finds a song in the database by the given ID and deletes it
   * 
   * @param SongId: The id of the song to search for
   * @return DbQueryStatus: The result of the search with the returned data and status
   */
  @Override
  public DbQueryStatus deleteSongById(String songId) {
    try {

      // Create new query to search by id
      Query query = new Query();
      query.addCriteria(Criteria.where("_id").is(songId));
      Song found = db.findAndRemove(query, Song.class);

      // If song delete it
      if (found != null) {
        return new DbQueryStatus("Delete Successful", DbQueryExecResult.QUERY_OK);
      }

      // If song does not exist return not found
      return new DbQueryStatus("Song not Found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

    } catch (Exception e) {

      // Return error status if failed
      return new DbQueryStatus("Could Not delete song", DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }

  /**
   * Finds a song in the database by the given ID and updates its favourites
   * 
   * @param SongId: The id of the song to search for
   * @param shouldDecrement: true/false depending on whether this should increment/decrement the
   *        likes
   * @return DbQueryStatus: The result of the search with the returned data and status
   */
  @Override
  public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
    try {

      // Check whether value shouldbe incremented or decremented
      long toAdd = 1;
      if (shouldDecrement) {
        toAdd = -1;
      }

      // Create new query to search by id
      Query query = new Query();
      query.addCriteria(Criteria.where("_id").is(songId));
      Song found = db.findOne(query, Song.class);

      // If song exists, update favourites count
      if (found != null) {
        toAdd = toAdd + found.getSongAmountFavourites();

        // Ensure add is valid
        if (toAdd >= 0) {
          found.setSongAmountFavourites(toAdd);
          db.findAndReplace(query, found);
          return new DbQueryStatus("Update Successful", DbQueryExecResult.QUERY_OK);
        }

        // Return error if negative likes
        return new DbQueryStatus("Cannot have negative favourites",
            DbQueryExecResult.QUERY_ERROR_GENERIC);
      }

      // If song does not exist return not found
      return new DbQueryStatus("Song not Found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

    } catch (Exception e) {

      // Return error status if failed
      return new DbQueryStatus("Could Not decrement/increment favourites",
          DbQueryExecResult.QUERY_ERROR_GENERIC);
    }
  }
}
