package se.tmeit.app.services;

import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import se.tmeit.app.R;
import se.tmeit.app.model.ExternalEvent;
import se.tmeit.app.model.ExternalEventAttendee;
import se.tmeit.app.model.Member;

/**
 * Downloads data entities from TMEIT web services.
 */
public final class Repository {

    private static final String HEADER_SERVICE_AUTH = "X-TMEIT-Service-Auth";
    private static final String HEADER_USERNAME = "X-TMEIT-Username";
    private static final String TAG = Repository.class.getSimpleName();
    private final String mServiceAuth;
    private final String mUsername;

    public Repository(String username, String serviceAuth) {
        mUsername = username;
        mServiceAuth = serviceAuth;
    }

    public void attendExternalEvent(int id, ExternalEventAttendee attendee, RepositoryResultHandler<Void> resultHandler) {
        try {
            Request request = new Request.Builder()
                    .url(TmeitServiceConfig.SERVICE_BASE_URL + "AttendExternalEvent.php")
                    .post(RequestBody.create(TmeitServiceConfig.JSON_MEDIA_TYPE, createJsonForAttendExternalEvent(id, attendee)))
                    .build();

            TmeitHttpClient.getInstance().enqueueRequest(request, new AttendExternalEventCallback(resultHandler));
        } catch (JSONException ex) {
            Log.e(TAG, "Unexpected JSON exception while creating request.", ex);
            resultHandler.onError(R.string.network_error_unspecified_protocol);
        }
    }

    public void getExternalEventDetails(int id, RepositoryResultHandler<ExternalEvent.RepositoryData> resultHandler) {
        Request request = getRequestBuilder("GetExternalEventDetails.php/" + id).build();
        TmeitHttpClient.getInstance().enqueueRequest(request, new GetExternalEventDetailsCallback(resultHandler));
    }

    public void getExternalEvents(RepositoryResultHandler<List<ExternalEvent>> resultHandler) {
        Request request = getRequestBuilder("GetExternalEvents.php").build();
        TmeitHttpClient.getInstance().enqueueRequest(request, new GetExternalEventsCallback(resultHandler));
    }

    public void getMembers(RepositoryResultHandler<Member.RepositoryData> resultHandler) {
        Request request = getRequestBuilder("GetMembers.php").build();
        TmeitHttpClient.getInstance().enqueueRequest(request, new GetMembersCallback(resultHandler));
    }

    Map<Integer, String> deserializeIdTitleMap(JSONArray jsonArray) throws JSONException {
        Map<Integer, String> result = new LinkedHashMap<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            result.put(obj.getInt(JsonKeys.ID), obj.getString(JsonKeys.TITLE));
        }
        return result;
    }

    private String createJsonForAttendExternalEvent(int id, ExternalEventAttendee attendee) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(JsonKeys.EVENT_ID, id);

        if (null != attendee) {
            JSONObject attending = new JSONObject();
            attending.put(JsonKeys.DOB, attendee.getDateOfBirth());
            attending.put(JsonKeys.DRINK_PREFS, attendee.getDrinkPreferences());
            attending.put(JsonKeys.FOOD_PREFS, attendee.getFoodPreferences());
            attending.put(JsonKeys.NOTES, attendee.getNotes());
            json.put(JsonKeys.ATTENDING, attending);
        }

        return json.toString();
    }

    private Request.Builder getRequestBuilder(String relativeUrl) {
        return new Request.Builder()
                .url(TmeitServiceConfig.SERVICE_BASE_URL + relativeUrl)
                .addHeader(HEADER_USERNAME, mUsername)
                .addHeader(HEADER_SERVICE_AUTH, mServiceAuth)
                .get();
    }

    private interface JsonKeys {
        String ATTENDEE = "attendee";
        String ATTENDING = "attending";
        String DOB = "dob";
        String DRINK_PREFS = "drink_prefs";
        String EVENT = "event";
        String EVENT_ID = "event_id";
        String FOOD_PREFS = "food_prefs";
        String GROUPS = "groups";
        String ID = "id";
        String NOTES = "notes";
        String TEAMS = "teams";
        String TITLE = "title";
        String TITLES = "titles";
        String USERS = "users";
    }

    private final class AttendExternalEventCallback extends GetResultCallback<Void> {
        public AttendExternalEventCallback(RepositoryResultHandler<Void> resultHandler) {
            super(resultHandler);
        }

        @Override
        protected Void getResult(JSONObject responseBody) throws JSONException {
            return null;
        }
    }

    private final class GetExternalEventDetailsCallback extends GetResultCallback<ExternalEvent.RepositoryData> {
        public GetExternalEventDetailsCallback(RepositoryResultHandler<ExternalEvent.RepositoryData> resultHandler) {
            super(resultHandler);
        }

        @Override
        protected ExternalEvent.RepositoryData getResult(JSONObject responseBody) throws JSONException {
            JSONObject jsonEvent = responseBody.getJSONObject(JsonKeys.EVENT);
            JSONObject jsonAttendee = responseBody.getJSONObject(JsonKeys.ATTENDEE);

            return new ExternalEvent.RepositoryData(ExternalEvent.fromJson(jsonEvent), ExternalEventAttendee.fromJson(jsonAttendee));
        }
    }

    private final class GetExternalEventsCallback extends GetResultCallback<List<ExternalEvent>> {
        private static final String EVENTS = "events";

        public GetExternalEventsCallback(RepositoryResultHandler<List<ExternalEvent>> resultHandler) {
            super(resultHandler);
        }

        @Override
        protected List<ExternalEvent> getResult(JSONObject responseBody) throws JSONException {
            JSONArray jsonEvents = responseBody.getJSONArray(EVENTS);

            ArrayList<ExternalEvent> events = new ArrayList<>();
            for (int i = 0; i < jsonEvents.length(); i++) {
                JSONObject jsonUser = jsonEvents.getJSONObject(i);
                events.add(ExternalEvent.fromJson(jsonUser));
            }

            return events;
        }
    }

    private final class GetMembersCallback extends GetResultCallback<Member.RepositoryData> {
        public GetMembersCallback(RepositoryResultHandler<Member.RepositoryData> resultHandler) {
            super(resultHandler);
        }

        @Override
        protected Member.RepositoryData getResult(JSONObject responseBody) throws JSONException {
            Map<Integer, String> groups = deserializeIdTitleMap(responseBody.getJSONArray(JsonKeys.GROUPS));
            Map<Integer, String> teams = deserializeIdTitleMap(responseBody.getJSONArray(JsonKeys.TEAMS));
            Map<Integer, String> titles = deserializeIdTitleMap(responseBody.getJSONArray(JsonKeys.TITLES));

            JSONArray jsonUsers = responseBody.getJSONArray(JsonKeys.USERS);

            ArrayList<Member> members = new ArrayList<>();
            for (int i = 0; i < jsonUsers.length(); i++) {
                JSONObject jsonUser = jsonUsers.getJSONObject(i);
                members.add(Member.fromJson(jsonUser));
            }

            return new Member.RepositoryData(members, groups, teams, titles);
        }
    }

    private abstract class GetResultCallback<TResult> implements Callback {
        protected final RepositoryResultHandler<TResult> mResultHandler;

        protected GetResultCallback(RepositoryResultHandler<TResult> resultHandler) {
            mResultHandler = resultHandler;
        }

        @Override
        public void onFailure(Request request, IOException e) {
            Log.e(TAG, "Downloading data failed due to an IO error.", e);
            mResultHandler.onError(R.string.repository_error_unspecified_network);
        }

        @Override
        public void onResponse(Response response) throws IOException {
            Log.i(TAG, "Download response received with HTTP status = " + response.code() + ", cached = " + (null == response.networkResponse()) + ".");

            if (!response.isSuccessful()) {
                Log.e(TAG, "Downloading data failed because an unsuccessful HTTP status code (" + response.code() + ") was returned.");
                mResultHandler.onError(R.string.repository_error_unspecified_protocol);
                return;
            }

            try {
                JSONObject responseBody = TmeitServiceConfig.getJsonBody(response, TAG);
                if (null != responseBody) {
                    TResult result = getResult(responseBody);
                    mResultHandler.onSuccess(result);
                } else {
                    mResultHandler.onError(R.string.repository_error_unspecified_protocol);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Downloading data failed due to a JSON error.", e);
                mResultHandler.onError(R.string.repository_error_unspecified_protocol);
            }
        }

        protected abstract TResult getResult(JSONObject responseBody) throws JSONException;
    }
}
