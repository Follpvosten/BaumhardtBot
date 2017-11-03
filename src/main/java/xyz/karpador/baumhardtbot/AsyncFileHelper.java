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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal wrapper for AsynchronousFileChannel, used to write and read from
 * the same file.
 * @author Follpvosten
 */
public class AsyncFileHelper {
    private ByteBuffer dataBuffer = null;
    private Future<Integer> currentResult = null;
    private AsynchronousFileChannel currentAfc = null;
    private final Path path;
    
    public AsyncFileHelper(String filePath) {
	path = Paths.get(filePath);
    }
    
    /**
     * Checks if the file passed to the constructor currently exists.
     * @return 
     */
    public boolean fileExists() {
	return Files.exists(path);
    }
    
    /**
     * Starts reading from the file passed to the constructor.
     */
    public void startRead() {
	try {
	    currentAfc = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
	    int fileSize = (int)currentAfc.size();
	    dataBuffer = ByteBuffer.allocate(fileSize);
	    
	    currentResult = currentAfc.read(dataBuffer, 0);
	} catch (IOException ex) {
	    Logger.getLogger(AsyncFileHelper.class.getName()).log(Level.SEVERE, null, ex);
	}
    }
    
    /**
     * Finishes the read process if needed and returns the content of the file.
     * @return 
     */
    public String getReadData() {
	if(dataBuffer == null || currentResult == null) return null;
	try {
	    currentAfc.close();
	    int readBytes = currentResult.get();
	    Logger.getLogger(AsyncFileHelper.class.getName())
		    .log(Level.INFO, String.format(
			    "Bytes read: %1$d from %2$s", 
			    readBytes, 
			    path.getFileName().toString())
		    );
	    
	    byte[] byteData = dataBuffer.array();
	    currentResult = null;
	    dataBuffer = null;
	    return new String(byteData, StandardCharsets.UTF_8);
	} catch (InterruptedException | ExecutionException | IOException ex) {
	    Logger.getLogger(AsyncFileHelper.class.getName()).log(Level.SEVERE, null, ex);
	}
	return null;
    }
    
    /**
     * Starts a writing process of the passed content to the file.
     * Finishes other read/write processes before, if any.
     * @param content 
     */
    public void startWrite(String content) {
	if(currentResult != null)
	    while(!currentResult.isDone())
		try {
		    Thread.sleep(50);
		} catch (InterruptedException ex) {
		    Logger.getLogger(AsyncFileHelper.class.getName()).log(Level.SEVERE, null, ex);
		}
	try {
	    currentAfc = AsynchronousFileChannel.open(path,
		StandardOpenOption.WRITE, StandardOpenOption.CREATE);
	    dataBuffer = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));
	    currentResult = currentAfc.write(dataBuffer, 0);
	} catch(IOException ex) {
	    Logger.getLogger(AsyncFileHelper.class.getName()).log(Level.SEVERE, null, ex);
	}
    }
}
