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

import java.util.ArrayList;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

/**
 *
 * @author Follpvosten
 */
public class BaumhardtPollingBot extends TelegramLongPollingBot {
    
    private final ArrayList<Integer> elizaUsers;
    
    public BaumhardtPollingBot() {
	elizaUsers = new ArrayList<>();
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
		} catch(TelegramApiException e) {
		    e.printStackTrace();
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
		    } catch(TelegramApiException e) {
			e.printStackTrace();
		    }
		}
	    }
	} else {
	    if(update.getMessage().getText().startsWith("/start")) {
		elizaUsers.add(update.getMessage().getFrom().getId());
		SendMessage message = new SendMessage()
		    .setChatId(update.getMessage().getChatId())
		    .setReplyToMessageId(update.getMessage().getMessageId())
		    .setText("Conversation started. Hello. ( ͡° ͜ʖ ͡°)");
		try {
		    sendMessage(message);
		} catch(TelegramApiException e) {
		    e.printStackTrace();
		}
	    } else if(update.getMessage().getText().contains("@" + getBotUsername())) {
		SendMessage message = new SendMessage()
		    .setChatId(update.getMessage().getChatId())
		    .setText("( ͡° ͜ʖ ͡°)");
		try {
		    sendMessage(message);
		} catch(TelegramApiException e) {
		    e.printStackTrace();
		}
	    }
	}
    }

    @Override
    public String getBotUsername() {
	return "baumhardtbot";
    }
    
}
