package piotr.cieplinski.polsl.pl.weatherrestclientexampleapp;
//Ta klasa definiuje adapter, który będzie wiązał dane z tablicy typu Weather z listą ListView

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Dziedziczymy po klasie ArrayAdapter
public class WeatherArrayAdapter extends ArrayAdapter<Weather> {

    //klasa reprezentująca widok pojedyńczego elementu ListView (list_item.xml)
    private static class ViewHolder {
        ImageView conditionImageView;
        TextView dayTextView;
        TextView lowTextView;
        TextView hiTextView;
        TextView humidityTextView;
    }

    //Przechowuje pobrane bitmmapy w pamięci podręcznej by aplikacja nie pobierała wielkorotnie tych samych obrazków,
    //Bitmapy będą przechowywane w pamięci podręcznej do momentu wyłączenia aplikacji
    private Map<String, Bitmap> bitmaps = new HashMap<>();

    //Tworząc obiekt Naszego adaptera musimy podać aktualny konteskt aplikacji i listę obiektów Weather,
    // które będą reprezentować prognozę pogody na kolejne dni
    public WeatherArrayAdapter(Context context, List<Weather> forecast) {
        //przekazujemy to do konstruktora klasy nadrzędnej (ArrayAdapter<Weather>)
        //kontekst określa aktywność, w której jest wyświetlana lista ListView, a forecast to lista danych do wyświetlenia
        //jako drugi parametr podajemy id rozkładu z jednym polem TextView, który ma być pojedyńczym elementem listy,
        //jeśli zamiast id rozkładu podamy wartość -1 to wskazujemy na zastosowanie rozkładu zawierającego wiecej niż jedno TextView
        super(context, -1, forecast);
    }

    //tworzenie zmodyfikowanego widoku dla elementów listy ListView
    //position - pozycja obiektu w tablicy danych,
    //convertView - widok reprezentujący element listy ListView
    //parent - element nadrzędny elementu convertView
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        //Uzyskanie dostępu do obiektu z listy danych na podstawie indeksu (position)
        Weather day = getItem(position);
        ViewHolder viewHolder; //Obiekt odwołujący się do elementów listy

        // sprawdź, czy istnieje nadający się do ponownego użycia obiekt ViewHolder przypisany do elementu
        // listy ListView usuniętego z ekranu; jeżeli obiekt taki nie istnieje, to utwórz nowy obiekt ViewHolder
        if(convertView == null) { // brak obiektu ViewHolder nadającego się do ponownego użycia;
            // utwórz nowy obiekt ViewHolder
            viewHolder = new ViewHolder();
            //Pozyskujemy obiekt LayoutInflanter, który jest powiązany z obiektem Context
            LayoutInflater inflater = LayoutInflater.from(getContext());
            //Obiekty klasy LayoutInflanter używamy do przygotowania do wyświetlenia rozkładów elementu ListView
            //Jako pierwszy arguiment podajemy id układu, który ma zostać wyświetlony jako element ListView
            //Jako drugi parametr podajemy element nadrzędny rozkładu z którym zostaną skojarzone widoki (kontrolki) rozkładu elementu ListView,
            //jako  trzeci jest podawana wartość logiczna czy widoki powinny być kojarzone z elementem parent automatycznie
            convertView = inflater.inflate(R.layout.list_item, parent, false);
            viewHolder.conditionImageView = (ImageView) convertView.findViewById(R.id.conditionImageView);
            viewHolder.dayTextView = (TextView) convertView.findViewById(R.id.dayTextView);
            viewHolder.hiTextView = (TextView) convertView.findViewById(R.id.hiTextView);
            viewHolder.humidityTextView = (TextView) convertView.findViewById(R.id.humidityTextView_);
            viewHolder.lowTextView = (TextView) convertView.findViewById(R.id.lowTextView);
            convertView.setTag(viewHolder);
        } else { // skorzystaj jeszcze raz z istniejącego obiektu ViewHolder przechowywanego jako znacznik elementu listy
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // jeżeli pobrano wcześniej ikonę ilustrującą daną pogodę, to skorzystaj z niej;
        // w przeciwnym wypadku pobierz ikonę za pomocą oddzielnego wątku
        if(bitmaps.containsKey(day.iconURL)) {
            viewHolder.conditionImageView.setImageBitmap(bitmaps.get(day.iconURL));
        } else {
            // pobierz i wyświetl obraz ilustrujący warunki pogodowe
            new LoadImageTask(viewHolder.conditionImageView).execute(day.iconURL);
        }
        // odczytaj pozostałe dane z obiektu Weather i umieść je w odpowiednich widokach
        Context context = getContext(); // do ładowania zasobów będących łańcuchami znaków
        viewHolder.dayTextView.setText(context.getString(R.string.day_description, day.dayOfWeek, day.description));
        viewHolder.lowTextView.setText(context.getString(R.string.low_temp, day.minTemp));
        viewHolder.hiTextView.setText(context.getString(R.string.high_temp, day.maxTemp));
        viewHolder.humidityTextView.setText(context.getString(R.string.humidity, day.humidity));

        return convertView; // zwróć kompletną listę elementów do wyświetlenia
    }

    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imageView;

        //przechowuje obiekt ImageView, na którym ma zaostać wyświetlona bitmapa
        public LoadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;

            try {
                URL url = new URL(params[0]); //utwórz obiekt URL obrazu
                // otwórz połączenie HttpURLConnection, uzyskaj dostęp do strumienia InputStream
                // i pobierz obraz
                connection = (HttpURLConnection) url.openConnection();
                try(InputStream inputStream =connection.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(inputStream); //Konwersja pobranego obrazka na bitmapę
                    bitmaps.put(params[0], bitmap); //zapisz w pamięci podręcznej
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                connection.disconnect();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            //przypisz ściągnętą bitmapę jako obraz źródłowy w ImageView elementu ListView
            imageView.setImageBitmap(bitmap);
        }
    }
}
