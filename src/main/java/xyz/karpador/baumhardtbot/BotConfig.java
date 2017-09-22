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

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Singleton class for loading and providing config values
 * @author Follpvosten
 */
public final class BotConfig {
    private static BotConfig instance;
    
    public static BotConfig getInstance() {
        if(instance == null)
            instance = new BotConfig();
        return instance;
    }
    
    private String telegramBotToken;
    
    public void init() {
        File configFile = new File("config.json");
        if(!configFile.exists()) {
            try {
                Files.copy(new File("config.example.json"), configFile);
            } catch(IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        try {
            String fileContent = FileUtils.readFileToString(configFile, "UTF-8");
            JSONObject configJson = new JSONObject(fileContent);
            JSONObject apiKeys = configJson.getJSONObject("api_tokens");
            telegramBotToken = apiKeys.getString("telegram");
        } catch(IOException | JSONException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public String getTelegramBotToken() {
        return telegramBotToken;
    }
    
}