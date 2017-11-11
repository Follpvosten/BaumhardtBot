/*
 * The MIT License
 *
 * Copyright 2017 Follpvosten.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package xyz.karpador.baumhardtbot;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Follpvosten
 */
public class CountdownList {
    private final List<LocalDateTime> dates;
    private final HashMap<Integer, LocalDateTime> userDates;
    
    public static final String DATEFORMATSTRING = "dd.MM.yyyy HH:mm";
    public static final DateTimeFormatter DATEFORMAT = DateTimeFormatter.ofPattern(DATEFORMATSTRING);
    
    public CountdownList() {
	dates = new ArrayList<>();
	userDates = new HashMap<>();
    }
    
    public String addDateTime(LocalDateTime dateTime) {
	dates.add(dateTime);
	return "Datetime " + dateTime.format(DATEFORMAT)
	    + " was added with index " + dates.indexOf(dateTime) + "!\n"
	    + "Use /setc " + dates.indexOf(dateTime) + " to track it.";
    }
    
    public String addUserDateTime(Integer userId, int index) {
	if(index > dates.size() - 1) {
	    return "Index muss zwischen 0 und " + (dates.size() - 1) + " liegen!";
	} else {
	    userDates.put(userId, dates.get(index));
	    return "Du verfolgst den Zeitpunkt " + dates.get(index).format(DATEFORMAT) + ".";
	}
    }
    
    public String getListForUser(Integer userId) {
	if(dates.size() < 1)
	    return "Keine Daten vorhanden.";
	String result = "";
	if(userDates.containsKey(userId)) {
	    for(int i = 0; i < dates.size(); i++) {
		result += String.valueOf(i) + " - " + dates.get(i).format(DATEFORMAT);
		if(dates.get(i) == userDates.get(userId))
		    result += " [x]";
		if(i < dates.size() - 1)
		    result += "\n";
	    }
	} else {
	    for(int i = 0; i < dates.size(); i++) {
		result += String.valueOf(i) + " - " + dates.get(i).format(DATEFORMAT);
		if(i < dates.size() - 1)
		    result += "\n";
	    }
	}
	return result;
    }
    
    private String genCountdownString(LocalDateTime dateTime) {
	String result = "Verbleibende Zeit bis " + dateTime.format(DATEFORMAT) + ": ";
	Duration duration = Duration.between(LocalDateTime.now(), dateTime);
	long seconds = duration.getSeconds();
	long absSeconds = Math.abs(seconds);
	result += String.format(
	    "%d Tage, %d Stunden, %d Minuten und %d Sekunden.",
	    absSeconds / 3600 / 24,
	    (absSeconds / 3600 % 24),
	    (absSeconds % 3600) / 60,
	    absSeconds % 60);
	return result;
    }
    
    public String getCountdown(int index) {
	if(index > dates.size() - 1)
	    return "Index muss zwischen 0 und " + (dates.size() - 1) + " liegen!";
	return genCountdownString(dates.get(index));
    }
    
    public String getCountdownForUser(Integer userId) {
	if(userDates.containsKey(userId)) {
	    return genCountdownString(userDates.get(userId));
	} else {
	    return "Bitte zuerst einen Countdown auswählen.";
	}
    }
    
    public HashMap<Integer, LocalDateTime> getElapsedEvents(LocalDateTime pointInTime) {
	HashMap<Integer, LocalDateTime> result = new HashMap<>();
	for(Entry<Integer, LocalDateTime> entry : userDates.entrySet()) {
	    // compareTo() < 1: Equal or greater
	    if(entry.getValue().compareTo(pointInTime) < 1) {
		result.put(entry.getKey(), entry.getValue());
	    }
	}
	return result;
    }
    
    public void purgeExpiredCountdowns() {
	LocalDateTime pointInTime = LocalDateTime.now();
	for(Entry<Integer, LocalDateTime> entry : userDates.entrySet()) {
	    // compareTo() > -1: Equal or greater
	    if(entry.getValue().compareTo(pointInTime) < 1) {
		userDates.remove(entry.getKey());
	    }
	}
	for(LocalDateTime time : dates) {
	    if(time.compareTo(pointInTime) < 1)
		dates.remove(time);
	}
    }
    
    public String toJsonString() {
	JSONObject result = new JSONObject();
	JSONArray datesJson = new JSONArray();
	for(LocalDateTime dateTime : dates) {
	    datesJson.put(dateTime.format(DATEFORMAT));
	}
	result.put("dates", datesJson);
	JSONArray userDatesJson = new JSONArray();
	for(Entry<Integer, LocalDateTime> entry : userDates.entrySet()) {
	    JSONObject userDateObject = new JSONObject();
	    userDateObject.put("key", entry.getKey());
	    userDateObject.put("value", entry.getValue().format(DATEFORMAT));
	    userDatesJson.put(userDateObject);
	}
	result.put("userDates", userDatesJson);
	return result.toString();
    }
    
    public static CountdownList fromJsonString(String jsonString) {
	CountdownList result = new CountdownList();
	JSONObject jsonObject = new JSONObject(jsonString);
	JSONArray datesArray = jsonObject.getJSONArray("dates");
	for(int i = 0; i < datesArray.length(); i++) {
	    result.dates.add(LocalDateTime.from(DATEFORMAT.parse(datesArray.getString(i))));
	}
	JSONArray userDatesArray = jsonObject.getJSONArray("userDates");
	for(int i = 0; i < userDatesArray.length(); i++) {
	    JSONObject userDateObject = userDatesArray.getJSONObject(i);
	    result.userDates.put(
		userDateObject.getInt("key"),
		LocalDateTime.from(DATEFORMAT.parse(userDateObject.getString("value")))
	    );
	}
	return result;
    }
}
