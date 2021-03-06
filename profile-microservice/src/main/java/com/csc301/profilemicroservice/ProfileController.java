package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@RestController
@RequestMapping("/")
public class ProfileController {
  public static final String KEY_USER_NAME = "userName";
  public static final String KEY_USER_FULLNAME = "fullName";
  public static final String KEY_USER_PASSWORD = "password";

  @Autowired
  private final ProfileDriverImpl profileDriver;

  @Autowired
  private final PlaylistDriverImpl playlistDriver;

  OkHttpClient client = new OkHttpClient();

  public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
    this.profileDriver = profileDriver;
    this.playlistDriver = playlistDriver;
  }

  @RequestMapping(value = "/profile", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> addProfile(@RequestParam Map<String, String> params,
      HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("POST %s", Utils.getUrl(request)));
    
    // check if all required params are present
    if (params.containsKey("userName") && params.containsKey("fullName")
        && params.containsKey("password")) {
      // create the user using the parameter values
      DbQueryStatus dbQueryStatus = profileDriver.createUserProfile(params.get(KEY_USER_NAME),
          params.get(KEY_USER_FULLNAME), params.get(KEY_USER_PASSWORD));

      response.put("message", dbQueryStatus.getMessage());
      response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(),
          dbQueryStatus.getData());
    } else {
      // return a response saying that invalid parameters have been provided
      response.put("message", "Invalid parameters given for addProfile");
      response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
    }

    return response;
    // end of my addition
  }

  @RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
  public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
      @PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("PUT %s", Utils.getUrl(request)));

    // my addition
    // call the followFriend function in driver
    DbQueryStatus dbQueryStatus = profileDriver.followFriend(userName, friendUserName);
    response.put("message", dbQueryStatus.getMessage());
    response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(),
        dbQueryStatus.getData());
    return response;
    // end of my addition
  }

  @RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
  public @ResponseBody Map<String, Object> getAllFriendFavouriteSongTitles(
      @PathVariable("userName") String userName, HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("PUT %s", Utils.getUrl(request)));

    try {
      // call the getAllSongsFriendsLike function in driver
      DbQueryStatus dbQueryStatus = profileDriver.getAllSongFriendsLike(userName);
      
      // if the function call for getting all songs is successfully executed
      if (dbQueryStatus.getdbQueryExecResult().equals(DbQueryExecResult.QUERY_OK)) {
        ObjectMapper dataMap = new ObjectMapper();
        Map<String, Object> dataResult = dataMap.convertValue(dbQueryStatus.getData(), Map.class);
        HashMap<String, Object> returnMap = new HashMap<>();
        ArrayList<String> songs;
        for (String s : dataResult.keySet()) {
          songs = new ArrayList<>();
          String songString = dataResult.get(s).toString();
          String[] songList = songString.substring(1, songString.length() - 1).split(", ");
          for (String d : songList) {
            if (!(d.isEmpty())) {
              HttpUrl.Builder urlBuilder =
                  HttpUrl.parse("http://localhost:3001" + "/getSongTitleById/" + d).newBuilder();
              String url = urlBuilder.build().toString();

              Request newRequest = new Request.Builder().url(url).method("GET", null).build();

              Call call = client.newCall(newRequest);
              Response responseFromMongo = call.execute();
              String responseValue = responseFromMongo.body().string();
              if (dataMap.readValue(responseValue, Map.class).get("status").toString()
                  .equals("OK")) {
                songs.add(dataMap.readValue(responseValue, Map.class).get("data").toString());

              } else {
                response.put("message", d + " was not found in MongoDb");
                response =
                    Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
                return response;
              }
            }
          }
          returnMap.put(s, songs.toArray());
        }
        dbQueryStatus.setMessage("Retrieval Successful");
        dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
        dbQueryStatus.setData(returnMap);

      }
      response.put("message", dbQueryStatus.getMessage());
      response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(),
          dbQueryStatus.getData());

    } catch (Exception e) {
      e.printStackTrace();
      response.put("message", "Failed to get all songs friends like");
      response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
    }

    return response;
  }

  @RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
  public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
      @PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("PUT %s", Utils.getUrl(request)));

    // my addition
    // call the unfollowFriend function in driver
    DbQueryStatus dbQueryStatus = profileDriver.unfollowFriend(userName, friendUserName);

    response.put("message", dbQueryStatus.getMessage());
    response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(),
        dbQueryStatus.getData());

    return response;
    // end of my addition
  }

  @RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
  public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
      @PathVariable("songId") String songId, HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("PUT %s", Utils.getUrl(request)));

    try {
      // call the likeSong function in driver
      DbQueryStatus dbQueryStatus = playlistDriver.likeSong(userName, songId);
      
      // if the function was called successfully, update the song count in the song
      if (dbQueryStatus.getdbQueryExecResult().equals(DbQueryExecResult.QUERY_OK)) {
        HttpUrl.Builder urlBuilder = HttpUrl
            .parse("http://localhost:3001" + "/updateSongFavouritesCount/" + songId).newBuilder();
        urlBuilder.addQueryParameter("shouldDecrement", "false");
        String url = urlBuilder.build().toString();
        RequestBody body = RequestBody.create(new byte[0], null);

        Request newRequest = new Request.Builder().url(url).method("PUT", body).build();

        Call call = client.newCall(newRequest);
        call.execute();
      } else if (dbQueryStatus.getdbQueryExecResult()
          .equals(DbQueryExecResult.QUERY_ERROR_NOT_FOUND)) {
        dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
      }
      response.put("message", dbQueryStatus.getMessage());
      response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(),
          dbQueryStatus.getData());

    } catch (Exception e) {
      response.put("message", "Failed to like song");
      response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
    }

    return response;
    // end of my addition
  }

  @RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
  public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
      @PathVariable("songId") String songId, HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("PUT %s", Utils.getUrl(request)));

    try {
      // call the unlikeSong function in driver

      DbQueryStatus dbQueryStatus = playlistDriver.unlikeSong(userName, songId);
      
      // if the function was called successfully, update the song count in the song
      if (dbQueryStatus.getdbQueryExecResult().equals(DbQueryExecResult.QUERY_OK)) {
        HttpUrl.Builder urlBuilder = HttpUrl
            .parse("http://localhost:3001" + "/updateSongFavouritesCount/" + songId).newBuilder();
        urlBuilder.addQueryParameter("shouldDecrement", "true");
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
      response.put("message", "Failed to unlike song");
      response = Utils.setResponseStatus(response, DbQueryExecResult.QUERY_ERROR_GENERIC, null);
    }

    return response;
    // end of my addition
  }

  @RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)
  public @ResponseBody Map<String, Object> deleteAllSongsFromDb(
      @PathVariable("songId") String songId, HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("PUT %s", Utils.getUrl(request)));

    // call the deleteSongFromDb function in driver
    DbQueryStatus dbQueryStatus = playlistDriver.deleteSongFromDb(songId);

    response.put("message", dbQueryStatus.getMessage());
    response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(),
        dbQueryStatus.getData());

    return response;
  }

  @RequestMapping(value = "/addSongProfile/{songId}", method = RequestMethod.PUT)
  public @ResponseBody Map<String, Object> addSongProfile(@PathVariable("songId") String songId,
      HttpServletRequest request) {

    Map<String, Object> response = new HashMap<String, Object>();
    response.put("path", String.format("PUT %s", Utils.getUrl(request)));
    // call the addSongProfile function in driver
    DbQueryStatus dbQueryStatus = playlistDriver.addSongProfile(songId);

    response.put("message", dbQueryStatus.getMessage());
    response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(),
        dbQueryStatus.getData());

    return response;
  }
}
