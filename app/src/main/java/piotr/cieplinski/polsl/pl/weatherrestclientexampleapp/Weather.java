package piotr.cieplinski.polsl.pl.weatherrestclientexampleapp;
//Ta klasa reprezentuje dane pogodowe jedngo dnia,
// dane pobrane w formacie JSON będziemy przetwarzać na tablicę obiektów tej klasy,
// gdzie każdy element tablicy będzie reprezentował jeden dzień

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class Weather {
    //pola klasy
    public final String dayOfWeek;
    public final String minTemp;
    public final String maxTemp;
    public final String humidity;
    public final String description;
    public final String iconURL;

    //Konstruktor
    public Weather(long timeStamp, double minTemp, double maxTemp, double humidity, String description, String iconName) {
        //NumberFormat wykorzystujemy do przekształcenia temperatury z double do integer
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(0);

        //Pozyskanie nazwy dnia tygodnia z daty w formacie TimeStamp
        this.dayOfWeek = convertTimeStampToDay(timeStamp);
        //wykorzystanie obiektu NumberFormats do pozyskania minTemp i maxTemp
        //Wygenerujemy takie żądanie do serwisu OpenWeatherMap.org, które nam zwróci temperaturę w skali Celsjusza - czyli nie musimy tutaj ncizego przeliczać
        //Serwis OpenWeatherMap.org domyślnie zwraca temperaturę w skali Kelwina, ale można także wymusić by zwrócił w skali Fahrenheita
        // sekwencja UNICODE "\u00B0" odpowiada znakowi stopnia
        this.minTemp = numberFormat.format(minTemp) + "\u00B0C";
        this.maxTemp = numberFormat.format(maxTemp) + "\u00B0C";
        //Usługa OpenWeatherMap.org zwraca wilgotność w procentach wyrażonych jako typ całkowitoliczbowy,
        //Z wykorzystaniem klasy NumberFormat możemy uzyskać ją jako ułamek dziesiętny dzieląc ją przez 100.0,
        // klasa NumberFormat zwraca ułamek dziesiętny w zapisie zgodnym z panujacym w aktualnej lokalizacji systemowej
        this.humidity = NumberFormat.getPercentInstance().format(humidity / 100.0);
        //opis to opis
        this.description = description;
        //generujemy link do ikony z serwisu openweathermap.org na podstawie nazwy zwróconej przez OpenWeatherMap.org
        this.iconURL = "http://openweathermap.org/img/w/" + iconName + ".png";
    }

    private static String convertTimeStampToDay(long timeStamp) {
        //Utworzenie obiektu klasy Calendar, który nam umożliwi w operowaniu na datach i czasie
        Calendar calendar = Calendar.getInstance();
        //ustawienie daty w milisekundach w obiekcie klasy Calendar,
        //Serwis OpenWeatherMap zwraca czas w sekundach więc przemnażając to przez 1000 przekształcamy go na milisekundy
        calendar.setTimeInMillis(timeStamp * 1000);
        //Utworzenie obiektu TimeZone dla strefy czasowej urządzenia
        TimeZone tz = TimeZone.getDefault();

        //dostosowanie czasu do strefy czasowej urządzenia
        calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));

//        Tworzymy obiekt klasy SimpleDateFormat do formatowania obiektu typu Date,
//        wartość "EEEE" parametru konstruktora powoduje, że obiekt SimpleDateFormat będzie przekształcał date na nazwę dnia tygodnia
        SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE, h:mm a");
        //zwracamy date zawartą w obiekcie klasy Calendar jako nazwę dnia tygodnia w Stringu
        return dateFormatter.format(calendar.getTime());
    }

    @Override
    public String toString() {
        return "dayOfWeek: " + dayOfWeek + " minTemp: " + minTemp + " maxTemp: " + maxTemp + " humidity: " + humidity + " description: " + description +  " iconURL: " + iconURL;
    }
}
