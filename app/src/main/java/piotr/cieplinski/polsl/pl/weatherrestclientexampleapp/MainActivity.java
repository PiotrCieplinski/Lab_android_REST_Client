package piotr.cieplinski.polsl.pl.weatherrestclientexampleapp;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private List<Weather> weatherList = new ArrayList<>();
    private WeatherArrayAdapter weatherArrayAdapter;
    private ListView weatherListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // automatycznie wygenerowany kod przygotowujący rozkład do wyświetlenia i konfigurujący pasek Toolbar
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // utwórz adapter ArrayAdapter łączący obiekt weatherList z widokiem weatherListView
        weatherListView = findViewById(R.id.weatherListView);
        weatherArrayAdapter = new WeatherArrayAdapter(this, weatherList);
        weatherListView.setAdapter(weatherArrayAdapter);

        // skonfiguruj FloatingActionButton służący do ukrywania klawiatury i wysłania żądania do usługi sieciowej
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // odczytaj tekst z obiektu locationEditText i utwórz obiekt URL odwołujący się do usługi sieciowej
                EditText locationEditText = findViewById(R.id.locationEditText);
                URL url = createURL(locationEditText.getText().toString());

                // ukryj klawiaturę i uruchom zadanie GetWeatherTask pobierające w oddzielnym wątku
                // dane prognozy pogody z serwisu OpenWeatherMap.org
                if(url != null) {
                    dismissKeyboard(locationEditText);
                    GetWeatherTask getLocalWeatherTask = new GetWeatherTask();
                    getLocalWeatherTask.execute(url);
                } else {
                    Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.invalid_url, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            }
        });
    }

    // ukryj klawiaturę, gdy użytkownik dotknie przycisku FloatingActionButtion
    private void dismissKeyboard(View view) {
        //metoda getSystemService pozwala na uzyskanie dostępu do usług systemowych
        //Dla każdej usługi jest zdefiniowana jakaś stała w klasie Context
        //My chcemy dostępu do klawiatury na ekranie
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //ukrywamy klawiaturę
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // skorzystaj z nazwy miasta i utwórz żądanie w postaci adresu URL kierowanego do serwisu openweathermap.org
    private URL createURL(String city) {
        String apiKey = getString(R.string.api_key);
        String baseUrl = getString(R.string.web_service_url);

        try {
            // utwórz adres URL dla wprowadzonego miasta i jednostek metrycznych (stopnie Celsjusza)
            //INFORMACJE ODNOSZĄCE SIĘ TYLKO DO SERWISU OpenWeatherMap.org, parametrów innych web-services należy szukać w ich dokumentacjach
            //parametr units określa jednostkę w jakiej ma zostać wyrażona temperatura, metric = Celsjusza, imperial = Fahrenheita,
            //standard bądź brak tego parametru oznacza Kelviny
            //parametr cnt określa na ile dni chcemy pogodę - domyślnie usługa zwraca na 7
            //APPID to nasz klucz API do serwisu OpenWeatherMap.org - bez tego aplikacja nie udośtępni nam żadnych danych
            //ewentualnie można dać jeszcze parametr mode, który określa w jakim formacie chcemy uzyskać dane,
            //domyślnie jest JSON, ale można wymusić mode=xml lub mode=html
            String urlString = baseUrl + URLEncoder.encode(city, "UTF-8") + "&units=metric&lang=pl&APPID=" + apiKey;
            return new URL(urlString);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null; //błąd tworzenia adresu URL
    }

    // generuje wywołanie usługi sieciowej REST w celu uzyskania danych prognozy pogody
    // i zapisania ich w lokalnym pliku HTML
    private class GetWeatherTask extends AsyncTask<URL, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(URL... params) {
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection) params[0].openConnection();
                int response = connection.getResponseCode();

                if(response == HttpURLConnection.HTTP_OK) {
                    StringBuilder builder = new StringBuilder();

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;

                        while((line = reader.readLine()) != null) {
                            builder.append(line);
                        }

                    } catch (IOException e) {
                        Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.read_error, Snackbar.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                    return new JSONObject(builder.toString());
                } else {
                    checkMessage(connection.getResponseMessage()); //sprawdzamy co poszło nie tak
                }
            } catch (Exception e) {
                Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.connect_error, Snackbar.LENGTH_LONG).show();
                e.printStackTrace();
            } finally {
                connection.disconnect(); // zamknij połączenie HttpURLConnection
            }

            return null;
        }

        // przetwórz odpowiedź w formacie JSON i aktualizuj obiekt ListView

        @Override
        protected void onPostExecute(JSONObject weather) {
            if(weather != null) {
                convertJSONtoArrayList(weather); // ponownie wypełnij obiekt weatherList
            } else {
                weatherList.clear(); //czyścimy listę ze starych danych
            }
            weatherArrayAdapter.notifyDataSetChanged(); // odśwież wiązania elementów listy ListView
            weatherListView.smoothScrollToPosition(0); // przewiń do góry
        }
    }

    // utwórz obiekty Weather na podstawie obiektu JSONObject zawierającego prognozę pogody
    private void convertJSONtoArrayList(JSONObject forecast) {
        weatherList.clear(); //czyścimy listę ze starych danych

        try {
            // odczytaj „listę” prognozy pogody — JSONArray
            JSONArray list = forecast.getJSONArray("list");

            // zamień każdy element listy na obiekt Weather
            for(int i = 0; i < list.length(); i++) {
                JSONObject day = list.getJSONObject(i); // odczytaj dane dotyczące jednego dnia
                // odczytaj z obiektu JSONObject dane dotyczące temperatury danego dnia ("temp")
                JSONObject temperatures = day.getJSONObject("main");
                // odczytaj z obiektu JSONObject opis i ikonę "weather”
                JSONObject weather = day.getJSONArray("weather").getJSONObject(0);
                // dodaj nowy obiekt Weather do listy weatherList
                weatherList.add(new Weather(day.getLong("dt"), // znacznik czasu i daty
                        temperatures.getDouble("temp_min"), // temperatura minimalna
                        temperatures.getDouble("temp_max"), // temperatura maksymalna
                        temperatures.getDouble("humidity"), // procentowa wilgotność powietrza
                        weather.getString("description"), // warunki pogodowe
                        weather.getString("icon"))); // nazwa ikony
            }
        } catch(JSONException e) {
            e.printStackTrace();
        }
    }

    private void checkMessage(String message) {
        switch(message) {
            case "Not Found":
                Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.city_not_found, Snackbar.LENGTH_LONG).show();
                break;
            default:
                Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.connect_error, Snackbar.LENGTH_LONG).show();
        }
    }
}
