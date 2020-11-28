package com.csc301.songmicroservice;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

@RestController
@RequestMapping("/")
public class SongController {

  @Autowired
  private final SongDal songDal;

  private OkHttpClient client = new OkHttpClient();


  public SongController(SongDal songDal) {
    this.songDal = songDal;
  }


  @RequestMapping(value = "/getSongById/{songId}", method = RequestMethod.GET)
  public @ResponseBody Map<String, Object> getSongById(@PathVariable("songId") String songId,
      HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("GET %s", Utils.getUrl(request)));

    DbQueryStatus dbQueryStatus = songDal.findSongById(songId);

    response.put("message", dbQueryStatus.getMessage());
    response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(),
        dbQueryStatus.getData());

    return response;
  }


  @RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)
  public @ResponseBody Map<String, Object> getSongTitleById(@PathVariable("songId") String songId,
      HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("GET %s", Utils.getUrl(request)));

    DbQueryStatus dbQueryStatus = songDal.getSongTitleById(songId);

    response.put("message", dbQueryStatus.getMessage());
    response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(),
        dbQueryStatus.getData());

    return response;
  }


  @RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE)
  public @ResponseBody Map<String, Object> deleteSongById(@PathVariable("songId") String songId,
      HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("DELETE %s", Utils.getUrl(request)));

    try {
      DbQueryStatus dbQueryStatus = songDal.deleteSongById(songId);
      if (dbQueryStatus.getdbQueryExecResult().equals(DbQueryExecResult.QUERY_OK)) {
        HttpUrl.Builder urlBuilder =
            HttpUrl.parse("http://localhost:3002" + "/deleteAllSongsFromDb/" + songId).newBuilder();
        String url = urlBuilder.build().toString();
        RequestBody body = RequestBody.create(new byte[0], null);

        Request newRequest = new Request.Builder().url(url).method("PUT", body).build();

        Call call = client.newCall(newRequest);
        call.execute();
      }

      response.put("message", dbQueryStatus.getMessage());
      response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(),
          dbQueryStatus.getData());

    } catch (Exception e) {
      e.printStackTrace();
      response.put("message", "Failed to remove song from database");
      response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
    }

    return response;
  }


  @RequestMapping(value = "/addSong", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> addSong(@RequestParam Map<String, String> params,
      HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("POST %s", Utils.getUrl(request)));

    if (params.containsKey("songName") && params.containsKey("songArtistFullName")
        && params.containsKey("songAlbum")) {
      try {
        Song newSong = new Song(params.get("songName"), params.get("songArtistFullName"),
            params.get("songAlbum"));

        ObjectId id = new ObjectId();
        newSong.setId(id);
        DbQueryStatus dbQueryStatus = songDal.addSong(newSong);

        if (dbQueryStatus.getdbQueryExecResult().equals(DbQueryExecResult.QUERY_OK)) {
          HttpUrl.Builder urlBuilder =
              HttpUrl.parse("http://localhost:3002" + "/addSong/" + id.toString()).newBuilder();
          String url = urlBuilder.build().toString();
          RequestBody body = RequestBody.create(new byte[0], null);

          Request newRequest = new Request.Builder().url(url).method("PUT", body).build();

          Call call = client.newCall(newRequest);
          call.execute();
        }
        response.put("message", dbQueryStatus.getMessage());
        response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(),
            dbQueryStatus.getData());

      } catch (Exception e) {
        response.put("message", "Failed to add song to database");
        response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
      }
    } else {
      response.put("message", "Invalid parameters given for addSong");
      response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
    }

    return response;
  }


  @RequestMapping(value = "/updateSongFavouritesCount/{songId}", method = RequestMethod.PUT)
  public @ResponseBody Map<String, Object> updateFavouritesCount(
      @PathVariable("songId") String songId,
      @RequestParam("shouldDecrement") String shouldDecrement, HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("data", String.format("PUT %s", Utils.getUrl(request)));

    boolean value = false;
    if (shouldDecrement.equals("true")) {
      value = true;
    } else if (!(shouldDecrement.equals("false"))) {
      response.put("message", "Invalid parameter: shouldDecrement only accepts true/false");
      response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
      return response;
    } else {
      value = false;
    }
    DbQueryStatus dbQueryStatus = songDal.updateSongFavouritesCount(songId, value);

    response.put("message", dbQueryStatus.getMessage());
    response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(),
        dbQueryStatus.getData());

    return response;
  }
}
