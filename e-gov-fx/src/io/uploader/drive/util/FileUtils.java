
package io.uploader.drive.util;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import java.nio.file.*;
import java.util.ArrayDeque;
import java.util.Queue;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.FileVisitResult.*;

public class FileUtils {
	
	final static Logger logger = LoggerFactory.getLogger(FileUtils.class);
	
	private FileUtils () { super () ; } ;
	
	public static final String emptyMd5 = "d41d8cd98f00b204e9800998ecf8427e" ;
	
	public static String getMD5 (File file) throws IOException
	{
		if (file.isDirectory())
			return emptyMd5 ;
		HashCode hc = com.google.common.io.Files.hash(file, Hashing.md5());
		return (hc != null) ? (hc.toString()) : (null) ;
	}
	
	
	public static String readAllAndgetMD5 (InputStream in) throws IOException
	{
		com.google.common.hash.HashingInputStream his = null ;
		try
		{
			his = new com.google.common.hash.HashingInputStream (Hashing.md5(), in) ;
			
			final int bufferSize = 2097152 ;
			final ReadableByteChannel inputChannel = Channels.newChannel(his);
			final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
			while (inputChannel.read(buffer) != -1) {
				buffer.clear();
			}
			/*
			byte[] bytesBuffer = new byte[bufferSize] ;
			int r = his.read(bytesBuffer, 0, bufferSize) ;
			while (r != -1)
				r = his.read(bytesBuffer) ;
			*/
			HashCode hc = his.hash() ;
			return (hc != null) ? (hc.toString()) : (null) ;
		}
		finally
		{
			if (his != null)
				his.close() ;
		}
	}
	
	
	public enum FileFinderOption {
		ALL,
		FILE_ONLY,
		DIRECTORY_ONLY,
	}
	
	
	public static Queue<Path> getAllFilesPath (Path srcDir, FileFinderOption option) throws IOException
	{
		Queue<Path> queue = new ArrayDeque<Path> () ;
		if (srcDir == null || !Files.isDirectory(srcDir))
			return queue ;
		
		TreePathFinder tpf = new TreePathFinder(queue, option);
		Files.walkFileTree(srcDir, tpf);
	
		return queue ;
	}
	
	
	public static BasicFileAttributes getFileAttr (Path path) throws IOException
	{
		BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class) ;
		return attr ;
	}
	
	
	private static class TreePathFinder extends SimpleFileVisitor<Path> {

		private final Queue<Path> filesPath ;
		private final FileFinderOption option ;
		
		public TreePathFinder(Queue<Path> filesPath, FileFinderOption option) {
			super();
			if (filesPath == null)
				throw new AssertionError ("TreePathFinder cannot accept null queue.") ;
			this.option = option ;
			this.filesPath = filesPath;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attr) 
		{
			if (option == FileFinderOption.ALL || option == FileFinderOption.FILE_ONLY) {
				filesPath.add(file) ;
			}
			return CONTINUE;
		}
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) 
		{
			if (option == FileFinderOption.ALL || option == FileFinderOption.DIRECTORY_ONLY) {
				filesPath.add(dir) ;
			}
			return CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
			return CONTINUE;
		}

		// If there is some error accessing
		// the file, let the user know.
		// If you don't override this method
		// and an error occurs, an IOException
		// is thrown.
		/*
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
            if (exc instanceof FileSystemLoopException) {
                logger.error("Error occurred while walking the directory to find all the files path (cycle detected: " + file + ").", exc);
            } else {
                logger.error("Error occurred while walking the directory to find all the files path.", exc);
            }
            return CONTINUE;
		}
		*/
	}
	
	
    public static class InputStreamProgressFilter extends FilterInputStream
    {
    	public static interface StreamProgressCallback
    	{
    		public void onStreamProgress (double progress) ;
    	}
    	
    	private final long size ;
    	private volatile long read = 0 ;
    	private final StreamProgressCallback callback ;
    	
		protected InputStreamProgressFilter(InputStream in, long size, StreamProgressCallback callback) {

			super(in);
			if (size < 0)
				throw new AssertionError ("The size of the stream to be minitored must be positive") ;
			
			// Note: we need the size because, in.available() does not necessarily return the size (see specs).
			this.size = size ;
			this.callback = callback ;
			checkProgress () ;
		}
    	
		private double getProgress ()
		{
			if (size == 0)
				return 1.0 ;
			return read / (double)size ;
		}
		
		private void checkProgress ()
		{
			if (callback == null)
				return ;
			callback.onStreamProgress(getProgress());
		}

		@Override
		public int read() throws IOException {
			int r = super.read();
			read += r ;
			checkProgress () ;
			return r;
		}

		@Override
		public int read(byte[] b) throws IOException {
			int r = super.read(b);
			read += r ;
			checkProgress () ;
			return r;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int r = super.read(b, off, len);
			read += r ;
			checkProgress () ;
			return r;
		}

		@Override
		public long skip(long n) throws IOException {
			long r = super.skip(n);
			read += r ;
			checkProgress () ;
			return r;
		}
    }
    
    
	public static InputStream getInputStreamWithProgressFilter(InputStreamProgressFilter.StreamProgressCallback callback, long size, Path path) throws FileNotFoundException 
	{
		return getInputStreamWithProgressFilter (callback, size, new FileInputStream(path.toFile())) ;
	}
	
	
	public static InputStream getInputStreamWithProgressFilter(InputStreamProgressFilter.StreamProgressCallback callback, long size, InputStream input)
	{
		if (input == null)
			return null ;
		InputStream in = new BufferedInputStream(
				new InputStreamProgressFilter(input, size, callback));
		return in;
	}
	
	
	public static void saveInputStreamInFile (InputStream input, File target, boolean closeInputStream) throws IOException
	{
		if (input == null || target == null)
			throw new AssertionError () ;
		FileOutputStream output = new FileOutputStream(target);
		try
		{
			IOUtils.copyLarge(input, output);
		}
		finally
		{
			if (output != null)
				output.close();
			if (closeInputStream)
				input.close();
		}
	}
	
	
    // http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
}
