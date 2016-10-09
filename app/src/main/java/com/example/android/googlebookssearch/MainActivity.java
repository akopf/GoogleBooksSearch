package com.example.android.googlebookssearch;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String GOOGLE_REQUEST_URL =
            "https://www.googleapis.com/books/v1/volumes?q=";
    private Button submit;
    private ArrayList<Book> searchBook;
    private ListView listView;
    private BookAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        searchBook = new ArrayList<>();
        listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(adapter = new BookAdapter(this, searchBook));
        submit = (Button) findViewById(R.id.button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isOnline()) {
                    new BookAsyncTask().execute();
                }
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Book currentBook = adapter.getItem(position);
                Uri bookUri = Uri.parse(currentBook.getUrl());
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, bookUri);
                startActivity(websiteIntent);
            }
        });

        if (savedInstanceState != null) {
            ArrayList<Book> books = (ArrayList<Book>) savedInstanceState.getSerializable("key");
            adapter.clear();
            adapter.addAll(books);
            adapter.notifyDataSetChanged();
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (isConnected) {
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        savedState.putSerializable("key", searchBook);
    }

    private class BookAsyncTask extends AsyncTask<URL, Void, ArrayList<Book>> {
        TextView search = (TextView) findViewById(R.id.text_edit);
        String query = search.getText().toString();

        @Override
        protected ArrayList<Book> doInBackground(URL... urls) {
            String[] searchQuery;
            if (query.contains(" ")) {
                searchQuery = query.split(" ");
                query = searchQuery[0] + searchQuery[1];
            }
            URL url = createUrl(GOOGLE_REQUEST_URL + query + "&maxResults=10");
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error connecting to the server", e);
            }
            ArrayList<Book> books = extractFeatureFromJson(jsonResponse);
            return books;
        }

        @Override
        protected void onPostExecute(ArrayList<Book> books) {
            listView.setAdapter(new BookAdapter(MainActivity.this, books));
            if (books == null) {
                return;
            }
        }

        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Error with creating URL", exception);
                return null;
            }
            return url;
        }

        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem retrieving the Google Books JSON results.", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            return jsonResponse;
        }

        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

        private ArrayList<Book> extractFeatureFromJson(String bookJSON) {
            ArrayList<Book> temp = new ArrayList<>();
            try {
                JSONObject baseJsonResponse = new JSONObject(bookJSON);
                JSONArray itemArray = baseJsonResponse.getJSONArray("items");
                for (int i = 0; i < itemArray.length(); i++) {
                    JSONObject responseObject = itemArray.getJSONObject(i);
                    JSONObject volumeInfo = responseObject.getJSONObject("volumeInfo");
                    String title = volumeInfo.getString("title");
                    String url = volumeInfo.getString("previewLink");
                    String author = "";
                    if (volumeInfo.has("authors")) {
                        JSONArray authors = volumeInfo.getJSONArray("authors");
                        author = authors.getString(0);
                    }
                    temp.add(new Book(title, author, url));
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing the search result JSON.");
            }
            return temp;
        }
    }
}


