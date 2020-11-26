package com.csc301.songmicroservice;

import static com.mongodb.client.model.Filters.eq;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

@Repository
public class SongDalImpl implements SongDal {

  private final MongoTemplate db;

  @Autowired
  public SongDalImpl(MongoTemplate mongoTemplate) {
    this.db = mongoTemplate;
  }

  // Method gets the collection to use for posts
  private MongoCollection<Document> getCollection() {
    try {
      db.createCollection("songs");
    } catch (Exception e) {
    }
    return db.getCollection("songs");
  }

  @Override
  public DbQueryStatus addSong(Song songToAdd) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DbQueryStatus findSongById(String songId) {

    DbQueryStatus dataToReturn;
    MongoCollection<Document> collection = getCollection();
    MongoCursor<Document> iterator;

    try {
      iterator = collection.find(eq("_id", new ObjectId(songId))).iterator();
    } catch (Exception e) {
      return new DbQueryStatus("Invalid format for song id",
          DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
    }

    if (iterator.hasNext()) {
      dataToReturn = new DbQueryStatus("Search successful", DbQueryExecResult.QUERY_OK);
      dataToReturn.setData(iterator.next());
    }

    return new DbQueryStatus("Song not Found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
  }

  @Override
  public DbQueryStatus getSongTitleById(String songId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DbQueryStatus deleteSongById(String songId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
    // TODO Auto-generated method stub
    return null;
  }
}
