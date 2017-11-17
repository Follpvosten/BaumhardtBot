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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

/**
 *
 * @author Follpvosten
 */
public final class BaumhardtPollingBot extends TelegramLongPollingBot {
    
    private final ArrayList<Integer> elizaUsers = new ArrayList<>();
    private final CountdownList countdownList;
    private boolean listInUse = false;
    
    private final AsyncFileHelper fileHelper;
    
    private class TimeBasedEvents implements Runnable {
	@Override
	public void run() {
	    // Check events per minute
	    try {
		synchronized(countdownList) {
		    if(listInUse) {
			countdownList.wait();
		    }
		    listInUse = true;
		    HashMap<Integer, LocalDateTime> events =
			    countdownList.getElapsedEvents(LocalDateTime.now());
		    for(Map.Entry<Integer, LocalDateTime> entry : events.entrySet()) {
			String answer = "Your tracked countdown expired at "
				+ CountdownList.DATEFORMAT.format(entry.getValue())
				+ "!";
			SendMessage message = new SendMessage()
			    .setChatId(entry.getKey().longValue())
			    .setText(answer);
			try {
			    sendMessage(message);
			} catch (TelegramApiException ex) {
			    Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.SEVERE, null, ex);
			}
		    }
		    countdownList.purgeExpiredCountdowns();
		    fileHelper.startWrite(countdownList.toJsonString());
		    listInUse = false;
		    countdownList.notifyAll();
		}
	    } catch(Exception ex) {
		listInUse = false;
		Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.SEVERE, null, ex);
	    }
	}
    }
    
    private final ScheduledExecutorService executor;
    
    public BaumhardtPollingBot() {
	fileHelper = new AsyncFileHelper("countdowns.json");
	if(fileHelper.fileExists()) {
	    String fileContent = null;
	    try {
		File file = new File("countdowns.json");
		byte[] data;
		try (FileInputStream fis = new FileInputStream(file)) {
		    data = new byte[(int) file.length()];
		    fis.read(data);
		}
		fileContent = new String(data, "UTF-8");
	    } catch (IOException ex) {
		Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.SEVERE, null, ex);
	    }
	    if(fileContent != null) {
		countdownList = CountdownList.fromJsonString(fileContent);
	    } else {
		countdownList = new CountdownList();
	    }
	} else {
	    countdownList = new CountdownList();
	}
	executor = Executors.newScheduledThreadPool(1);
	Calendar currentCal = Calendar.getInstance();
	currentCal.set(Calendar.SECOND, 0);
	currentCal.set(Calendar.MILLISECOND, 0);
	currentCal.add(Calendar.MINUTE, 1);
	long initialDelay = currentCal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
	// Execute time based events every 60 seconds, starting with the next minute
	executor.scheduleAtFixedRate(new TimeBasedEvents(), initialDelay, 60 * 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public String getBotToken() {
	return BotConfig.getInstance().getTelegramBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
	if(!update.hasMessage()) return;
	if(!update.getMessage().hasText()) return;
	if(elizaUsers.contains(update.getMessage().getFrom().getId())) {
	    if(update.getMessage().getText().startsWith("/stop")) {
		elizaUsers.remove(update.getMessage().getFrom().getId());
		SendMessage message = new SendMessage()
		    .setChatId(update.getMessage().getChatId())
		    .setReplyToMessageId(update.getMessage().getMessageId())
		    .setText("Conversation stopped. Bye! ( ͡° ͜ʖ ͡°)");
		try {
		    sendMessage(message);
		} catch(TelegramApiException ex) {
		    Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.SEVERE, null, ex);
		}
	    } else {
		String result = Eliza.processInput(update.getMessage().getText());
		if(result != null) {
		    SendMessage message = new SendMessage()
			.setChatId(update.getMessage().getChatId())
			.setReplyToMessageId(update.getMessage().getMessageId())
			.setText(result);
		    try {
			sendMessage(message);
		    } catch(TelegramApiException ex) {
			Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.SEVERE, null, ex);
		    }
		}
	    }
	} else {
	    String messageText = update.getMessage().getText();
	    if(messageText.startsWith("/start")) {
		elizaUsers.add(update.getMessage().getFrom().getId());
		SendMessage message = new SendMessage()
		    .setChatId(update.getMessage().getChatId())
		    .setReplyToMessageId(update.getMessage().getMessageId())
		    .setText("Conversation started. Hello. ( ͡° ͜ʖ ͡°)");
		try {
		    sendMessage(message);
		} catch(TelegramApiException ex) {
		    Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.SEVERE, null, ex);
		}
	    } else if(messageText.contains("@" + getBotUsername()) && !messageText.startsWith("/")) {
		SendMessage message = new SendMessage()
		    .setChatId(update.getMessage().getChatId())
		    .setText("( ͡° ͜ʖ ͡°)");
		try {
		    sendMessage(message);
		} catch(TelegramApiException ex) {
		    Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.SEVERE, null, ex);
		}
	    } else if(messageText.startsWith("/")) {
		String[] messageComponents = messageText.split(" ");
		String command = messageComponents[0].replace("/", "");
		if(command.contains("@")) {
		    if(!command.split("@")[1].equals(getBotUsername()))
			return;
		    command = command.split("@")[0];
		}
		String args =
			messageComponents.length > 1
			? messageText.substring(messageText.indexOf(" ") + 1)
			: null;
		synchronized(countdownList) {
		    while(listInUse) {
			try {
			    countdownList.wait();
			} catch (InterruptedException ex) {
			    Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.SEVERE, null, ex);
			}
		    }
		    listInUse = true;
		    switch (command) {
			case "help":
			    {
				String answer = "Available commands:\n"
					+ "/start: Start a conversation\n"
					+ "/stop: Stop the running conversation\n"
					+ "/listc: List existing countdown dates with index\n"
					+ "/putc <date>: Submit a new countdown date and time (Format: dd.MM.yyyy HH:mm)\n"
					+ "/setc <index>: Set your tracked countdown (using an index from /list)\n"
					+ "/getc [index]: Get a countdown. If no index is provided, your tracked countdown is returned.";
				SendMessage message = new SendMessage()
				    .setChatId(update.getMessage().getChatId())
				    .setReplyToMessageId(update.getMessage().getMessageId())
				    .setText(answer);
				try {
				    sendMessage(message);
				} catch (TelegramApiException ex) {
				    Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.SEVERE, null, ex);
				}
				break;
			    }
			case "listc":
			    {
				String answer = countdownList.getListForUser(update.getMessage().getFrom().getId());
				SendMessage message = new SendMessage()
				    .setChatId(update.getMessage().getChatId())
				    .setReplyToMessageId(update.getMessage().getMessageId())
				    .setText(answer);
				try {
				    sendMessage(message);
				} catch (TelegramApiException ex) {
				    Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.SEVERE, null, ex);
				}
				break;
			    }
			case "putc":
			    {
				String answer;
				try {
				    if(args == null)
					answer = "Please submit a date!";
				    else {
					LocalDateTime dateTime = LocalDateTime.from(CountdownList.DATEFORMAT.parse(args));
					answer = countdownList.addDateTime(dateTime);
					fileHelper.startWrite(countdownList.toJsonString());
				    }
				} catch(DateTimeParseException ex) {
				    Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.INFO, null, ex);
				    answer = "Please provide a valid date! Format: " + CountdownList.DATEFORMATSTRING;
				}
				SendMessage message = new SendMessage()
				    .setChatId(update.getMessage().getChatId())
				    .setReplyToMessageId(update.getMessage().getMessageId())
				    .setText(answer);
				try {
				    sendMessage(message);
				} catch (TelegramApiException ex) {
				    Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.SEVERE, null, ex);
				}
				break;
			    }
			case "setc":
			    {
				String answer;
				try {
				    if(messageComponents.length < 2)
					answer = "Please submit a number!";
				    else {
					answer = countdownList.addUserDateTime(
					    update.getMessage().getFrom().getId(),
					    Integer.parseInt(messageComponents[1])
					);
					fileHelper.startWrite(countdownList.toJsonString());
				    }
				} catch(NumberFormatException ex) {
				    Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.INFO, null, ex);
				    answer = "Bitte eine gültige Zahl angeben!";
				}
				SendMessage message = new SendMessage()
				    .setChatId(update.getMessage().getChatId())
				    .setReplyToMessageId(update.getMessage().getMessageId())
				    .setText(answer);
				try {
				    sendMessage(message);
				} catch (TelegramApiException ex) {
				    Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.SEVERE, null, ex);
				}
				break;
			    }
			case "getc":
			    {
				String answer;
				if(messageComponents.length > 1) {
				    try {
					answer = countdownList.getCountdown(Integer.parseInt(messageComponents[1]));
				    } catch(NumberFormatException ex) {
					Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.INFO, null, ex);
					answer = "Bitte eine gültige oder keine Zahl angeben!";
				    }
				} else {
				    answer = countdownList.getCountdownForUser(update.getMessage().getFrom().getId());
				}
				SendMessage message = new SendMessage()
				    .setChatId(update.getMessage().getChatId())
				    .setReplyToMessageId(update.getMessage().getMessageId())
				    .setText(answer);
				try {
				    sendMessage(message);
				} catch (TelegramApiException ex) {
				    Logger.getLogger(BaumhardtPollingBot.class.getName()).log(Level.SEVERE, null, ex);
				}
				break;
			    }
			default:
			    break;
		    }
		    listInUse = false;
		    countdownList.notifyAll();
		}
	    }
	}
    }

    @Override
    public String getBotUsername() {
	return "baumhardtbot";
    }
    
}
