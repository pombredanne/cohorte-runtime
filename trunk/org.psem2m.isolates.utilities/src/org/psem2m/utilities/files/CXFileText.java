package org.psem2m.utilities.files;
//
import java.io.File;
import java.io.FileInputStream;
import java.io.PushbackInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.psem2m.utilities.CXOSUtils;
/**
 * Classe de gestion de fichiers de type TEXT
 * 
 * Prend en compte la gestion des fichiers unicode en lecture et ecriture
 * --> Lit les 'Byte Order Mark' (BOM) suivants :
 * -----> UTF_8, UTF_16BE, UTF_16LE, UTF_32BE, UTF_32LE
 * --> Codage par defaut getDefaultEncoding()=ENCODING_ISO_8859_1
 * 
 * Ecriture
 * --> Si le fichier est trouve, on conserve le BOM existant
 * 
 * !! MonoThread
 */
public class CXFileText extends CXFile
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3258125856164622641L;
	/*
	 * Taille du Byte Order Mark poour les fichiers unicode
	 * Voir http://www.unicode.org/unicode/faq/utf_bom.html
	 */
	private static int BOM_SIZE = 4;
	private static byte BOM_UTF_16BE[] = {(byte) 0xFE, (byte) 0xFF };
	private static byte BOM_UTF_16LE[] = {(byte) 0xFF, (byte) 0xFE };
	private static byte BOM_UTF_32BE[] = {(byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF };
	private static byte BOM_UTF_32LE[] = {(byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00 };
	private static byte BOM_UTF_8[] = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
	/*
	 * Java
	 */
	public final static String ENCODING_ISO_8859_1 = CXOSUtils.ENCODING_ISO_8859_1;
	public final static String ENCODING_UTF_16BE = "UTF-16BE";
	public final static String ENCODING_UTF_16LE = "UTF-16LE";
	public final static String ENCODING_UTF_32BE = "UTF-32BE";
	public final static String ENCODING_UTF_32LE = "UTF-32LE";
	public final static String ENCODING_UTF_8 = "UTF-8";
	/*
	 * Voir comment gerer JSHARP
	 */
	public final static String UTF_8_CONSTANT_JSHARP = "UTF8";
	
	public final static boolean APPEND = true;

	/**
	 * True si aBomRef est contenu dans aBom
	 * 
	 * @param aBom
	 * @param aBomRef
	 * @return
	 */
	private static boolean checkBOM(byte[] aBom, byte[] aBomRef)
	{
	  // 16w_000 - Fiche 45147 - Corrections des utilitaires SHARED
	  if (aBom==null || aBomRef==null) return false;
		int wI = 0;
		while (wI < aBomRef.length && aBom[wI] == aBomRef[wI])
			wI++;
		return wI == aBomRef.length;
	}

	
	
	/**
	 * 16j_103
	 * 
	 * @param aBuffer
	 * @return l'encoding au vu des premier octets du buffer
	 */
	public static String readEncoding(byte[] aBuffer) {
		return readEncoding(aBuffer, CXOSUtils.getDefaultFileEncoding());
	}

	/**
	 * 16j_103
	 * 
	 * @param aBuffer
	 * @param aDefaultEncoding
	 * @return l'encoding au vu des premiers octets du buffer
	 */
	public static String readEncoding(byte[] aBuffer, String aDefaultEncoding) {
		String wEncoding = null;
		if (checkBOM(aBuffer, BOM_UTF_8)) {
			wEncoding = ENCODING_UTF_8;
		} else if (checkBOM(aBuffer, BOM_UTF_16BE)) {
			wEncoding = ENCODING_UTF_16BE;
		} else if (checkBOM(aBuffer, BOM_UTF_16LE)) {
			wEncoding = ENCODING_UTF_16LE;
		} else if (checkBOM(aBuffer, BOM_UTF_32BE)) {
			wEncoding = ENCODING_UTF_32BE;
		} else if (checkBOM(aBuffer, BOM_UTF_32LE)) {
			wEncoding = ENCODING_UTF_32LE;
		}

		if (wEncoding == null) {
			wEncoding = (aDefaultEncoding!=null)?aDefaultEncoding:CXOSUtils.getDefaultFileEncoding();
		}
		return wEncoding;
	}
	
	/**
	 * Lit le BOM du fichier et renvoie l'encodage
	 * Pas de BOM --> Renvoie l'encodage par defaut du systeme
	 * Methode Static pour etre sur qu'elle n'altere aucune propriete de la classe
	 * @param aUnreadStream stream pour lecture (gere l'unread de entete)
	 * aUnreadStream=null -> Creation et close d'une stream
	 * @param aFail=true - Exception si erreur
	 * aFail=false --> Renvoie getDefaultEncoding() si erreur
	 * @return l'encodage du fichier texte
	 * @throws Exception
	 */
	protected static String readEncoding(File aFile, PushbackInputStream aUnreadStream, boolean aFail) throws Exception
	{
		return readEncoding(aFile, aUnreadStream, null, aFail);
	}
	
	/**
	 * Lit le BOM du fichier et renvoie l'encodage
	 * Pas de BOM --> Renvoie aDefaultEncoding
	 * Methode Static pour etre sur qu'elle n'altere aucune propriete de la classe
	 * @param aUnreadStream stream pour lecture (gere l'unread de entete)
	 * aUnreadStream=null -> Creation et close d'une stream
	 * @param aDefaultEncoding encodage par defaut si BOM non trouve
	 * aDefaultEncoding=null -> On choisit l'encodage par defaut du systeme
	 * @param aFail=true - Exception si erreur
	 * aFail=false --> Renvoie getDefaultEncoding() si erreur
	 * @return l'encodage du fichier texte
	 * @throws Exception
	 */
	protected static String readEncoding(File aFile, PushbackInputStream aUnreadStream, String aDefaultEncoding, boolean aFail) throws Exception
	{
		PushbackInputStream wUnreadStream = aUnreadStream;
		String wDefEncod = aDefaultEncoding != null ? aDefaultEncoding : CXOSUtils.getDefaultFileEncoding();
		String wEncoding = null;
		try
		{
			if (wUnreadStream == null)
				wUnreadStream = new PushbackInputStream(new FileInputStream(aFile), BOM_SIZE);

			byte bom[] = new byte[BOM_SIZE];
			int n, unread;
			n = wUnreadStream.read(bom, 0, bom.length);

			if (checkBOM(bom, BOM_UTF_8))
			{
				wEncoding = ENCODING_UTF_8;
				unread = n - 3;
			}
			else if (checkBOM(bom, BOM_UTF_16BE))
			{
				wEncoding = ENCODING_UTF_16BE;
				unread = n - 2;
			}
			else if (checkBOM(bom, BOM_UTF_16LE))
			{
				wEncoding = ENCODING_UTF_16LE;
				unread = n - 2;
			}
			else if (checkBOM(bom, BOM_UTF_32BE))
			{
				wEncoding = ENCODING_UTF_32BE;
				unread = n - 4;
			}
			else if (checkBOM(bom, BOM_UTF_32LE))
			{
				wEncoding = ENCODING_UTF_32LE;
				unread = n - 4;
			}
			else
			{
				// Unicode BOM mark not found, unread all bytes
				unread = n;
			}
			if (wEncoding != null)
      {
        // System.out.println("Found BOM " + wEncoding + ", read=" + n + ", unread=" + unread);
        //System.out.println("BOM Found - " + wEncoding + " - " + aFile.getAbsolutePath());
      }
			else
			{
				wEncoding = wDefEncod;
				// System.out.println("No BOM found - encoding=" + wEncoding + ", read=" + n + ", unread=" + unread);
				//System.out.println("BOM not found - encoding=" + wEncoding + " - " + aFile.getAbsolutePath());
			}

			if (unread > 0)
				wUnreadStream.unread(bom, (n - unread), unread);
		}
		catch (Exception e)
		{
			if (aFail)
				throw e;
			else
				wEncoding = wDefEncod;
		}
		finally
		{
			if (aUnreadStream == null && wUnreadStream != null)
				wUnreadStream.close();
		}
		return wEncoding;
	}
	
	/*
	 * 
	 */
	private BufferedReader pBufReader = null;
	private BufferedWriter pBufWriter = null;
	/*
	 * Encodage par defaut --> Celui du systeme
	 */
	private String pDefaultEncoding = CXOSUtils.getDefaultFileEncoding();
	/*
	 * Encodage du fichier - Initialialise e l'ouverture (openReadLine et openAppend)
	 */
	private String pEncoding = null;
	private InputStreamReader pFileReader = null;
	private OutputStreamWriter pFileWriter = null;
	/*
	 * Gestion de l'encoding des fichiers existants
	 */
	private boolean pKeepExistingBOM = true;
	private PushbackInputStream pUnreadStream = null;

	/**
	 * @param aFile
	 */
	public CXFileText(CXFile aFile)
	{
		super(aFile);
	}
	/**
	 * 16w_104 - suppression des ajout systematique d'un "separatorChar" en fin de path d'un Dir !
	 * 
	 * @param aParentDir
	 * @param aFileName
	 */
	public CXFileText(CXFileDir aParentDir, String aFileName)
	{
		super(aParentDir, aFileName);
	}
	/**
	 * @param aFullPath
	 */
	public CXFileText(String aFullPath)
	{
		super(aFullPath);
	}
	/**
	 * @param aParentDir
	 * @param aFileName
	 */
	public CXFileText(String aParentDir, String aFileName)
	{
		super(aParentDir, aFileName);
	}
	/* --------------------------------------------------------------------
	 * 
	 * (non-Javadoc)
	 * @see java.io.File#canWrite()
	 */
	@Override
	public boolean canWrite()
	{
		return isOpenWrite() && super.canWrite();
	}
	/**
	 * @throws Exception
	 */
	private void checkWrite() throws Exception
	{
		if (!canWrite())
			throw new IOException("File not opened - Can't write into file '" + getAbsolutePath() + "'");
	}
	/* --------------------------------------------------------------------
	 * 
	 * (non-Javadoc)
	 * @see com.adonix.adminsrv.utils.CXFile#close()
	 */
	@Override
	public void close()
	{
		try
		{
			super.close();
			if (pFileReader != null)
				pFileReader.close();
			if (pBufReader != null)
				pBufReader.close();
			if (pUnreadStream != null)
				pUnreadStream.close();
			if (pFileWriter != null)
				pFileWriter.close();
			if (pBufWriter != null)
				pBufWriter.close();
		}
		catch (Throwable e)
		{
		}
		pFileReader = null;
		pBufReader = null;
		pUnreadStream = null;
		pFileWriter = null;
		pBufWriter = null;
		pUnreadStream = null;
		pEncoding = null;
	}
	/**
	 * @return
	 */
	public String getDefaultEncoding()
	{
		return pDefaultEncoding;
	}
	/**
	 * @return
	 */
	public String getEncoding()
	{
		if (pFileReader != null)
			return pFileReader.getEncoding();
		else if (pFileWriter != null)
			return pFileWriter.getEncoding();
		else
			return pEncoding;
	}
	/**
	 * @throws Exception
	 */
	protected void initStreamReader() throws Exception
	{
		if (pFileReader != null)
			return;
		if (!canRead())
			throw new IOException("Can't read file '" + getAbsolutePath() + "'");
		// Lit le BOM et renvoie l'encodage
		pUnreadStream = new PushbackInputStream(getInputStream(), BOM_SIZE);
		pEncoding = readEncoding(this, pUnreadStream, getDefaultEncoding(), true);
		// Use given encoding - Jamais null - Au cas oe...
		if (pEncoding == null)
			pFileReader = new InputStreamReader(pUnreadStream);
		else
			pFileReader = new InputStreamReader(pUnreadStream, pEncoding);
		pBufReader = new BufferedReader(pFileReader);
	}
	/**
	 * Sauvegarde de la 1ere version qui fonctionnait 
	 * http://koti.mbnet.fi/akini/java/unicodereader/UnicodeInputStream.java.txt
	 * @throws Exception
	 */
	protected void initStreamReaderSVG() throws Exception
	{
		if (pFileReader != null)
			return;

		if (!canRead())
			throw new IOException("Can't read file '" + getAbsolutePath() + "'");

		pUnreadStream = new PushbackInputStream(getInputStream(), BOM_SIZE);

		String encoding = null;
		byte bom[] = new byte[BOM_SIZE];
		int n, unread;
		n = pUnreadStream.read(bom, 0, bom.length);

		if ((bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF))
		{
			encoding = ENCODING_UTF_8;
			unread = n - 3;
		}
		else if ((bom[0] == (byte) 0xFE) && (bom[1] == (byte) 0xFF))
		{
			encoding = ENCODING_UTF_16BE;
			unread = n - 2;
		}
		else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE))
		{
			encoding = ENCODING_UTF_16LE;
			unread = n - 2;
		}
		else if ((bom[0] == (byte) 0x00) && (bom[1] == (byte) 0x00) && (bom[2] == (byte) 0xFE) && (bom[3] == (byte) 0xFF))
		{
			encoding = ENCODING_UTF_32BE;
			unread = n - 4;
		}
		else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE) && (bom[2] == (byte) 0x00) && (bom[3] == (byte) 0x00))
		{
			encoding = ENCODING_UTF_32LE;
			unread = n - 4;
		}
		else
		{
			// Unicode BOM mark not found, unread all bytes
			unread = n;
		}
		if (encoding != null)
			System.out.println("Found BOM " + encoding + ", read=" + n + ", unread=" + unread);
		else
		{
			encoding = CXOSUtils.getDefaultFileEncoding();
			System.out.println("No BOM found - encoding=" + encoding + ", read=" + n + ", unread=" + unread);
		}

		if (unread > 0)
			pUnreadStream.unread(bom, (n - unread), unread);

		// Use given encoding
		if (encoding == null)
			pFileReader = new InputStreamReader(pUnreadStream);
		else
			pFileReader = new InputStreamReader(pUnreadStream, encoding);
		pBufReader = new BufferedReader(pFileReader);
	}
	/**
	 * @return
	 */
	public boolean isOpen()
	{
		return isOpenReadLine() || isOpenWrite();
	}
	/**
	 * @return
	 */
	public boolean isOpenReadLine()
	{
		return pFileReader != null && pBufReader != null;
	}
	/**
	 * @return
	 */
	public boolean isOpenWrite()
	{
		return pFileWriter != null && pBufWriter != null;
	}
	/**
	 * @throws Exception
	 */
	public void openAppend() throws Exception
	{
		openWrite(true, null);
	}
	/**
	 * @param aEncoding
	 * @throws Exception
	 */
	public void openAppend(String aEncoding) throws Exception
	{
		openWrite(true, aEncoding);
	}
	/**
	 * @throws Exception
	 */
	public void openReadLine() throws Exception
	{
		close();
		initStreamReader();
	}
	/**
	 * @throws Exception
	 */
	public void openWrite() throws Exception
	{
		openWrite(false, null);
	}
	/**
	 * Ouvre un fichier text en lecture
	 * @param aAppend - true --> Ecriture en fin de fichier
	 * @param aAppend - false --> Creation d'un nouveau fichier
	 * @param aEncoding - Encodage du fichier
	 * aEncoding=null encodage par defaut
	 * aEncoding=null et mode append --> Lecture du BOM dans le fichier existant ou encodage par defaut
	 */
	public void openWrite(boolean aAppend, String aEncoding) throws Exception
	{
		close();
		//	Mise e jour pEncoding
		if (aEncoding != null)
			pEncoding = aEncoding;
		// En mode !append et si le fichier existz on peut conserver ou non le BOM du fichier (pKeepExistingBOM)
		// pKeepExistingBOM=true par defaut --> Si on reecrit un fichier que l'on n'a pas cree on conserve le BOM
		// --> L'application qui utilise se fichier verra le meme BOM que le fichier d'origine
		else if (exists() && (aAppend || pKeepExistingBOM))
			//	Lit l'encodage - Lecture du BOM - getDefaultEncoding() si pas de BOM ou Fail
			pEncoding = readEncoding(this, null, getDefaultEncoding(), false);
		else
			pEncoding = getDefaultEncoding();

		if (exists() && !aAppend)
		{
			// Sauvegarde de l'encodage
			this.writeEncoding();
		}
		else if (!(exists() && aAppend))
		{
			this.createNewFile();
			this.writeEncoding();
		}

		pFileWriter = new OutputStreamWriter(getOutputStream(true), pEncoding);
		pBufWriter = new BufferedWriter(pFileWriter);
	}
	/**
	 * @param aEncoding
	 * @throws Exception
	 */
	public void openWrite(String aEncoding) throws Exception
	{
		openWrite(false, aEncoding);
	}
	/**
	 * @param aAppend
	 * @throws Exception
	 */
	public void openWrite(boolean aAppend) throws Exception
	{
		openWrite(aAppend, null);
	}
	/**
	 * @return
	 * @throws Exception
	 */
	public String readAll() throws Exception
	{
		close();
		initStreamReader();
		String wRes;
		int wMax = pUnreadStream.available();
		if (wMax == 0)
			wRes = new String("");
		else
		{
			byte[] wData = new byte[wMax];
			pUnreadStream.read(wData);
			pUnreadStream.close();
			wRes = new String(wData, pEncoding);
		}
		close();
		return wRes;
	}
	/**
	 * @param aEncoding
	 * @return
	 * @throws Exception
	 */
	public String readAll(String aEncoding) throws Exception
	{
		close();
		return new String(readAllBytes(), aEncoding);
	}
	/**
	 * @return
	 * @throws Exception
	 */
	public String readLine() throws Exception
	{
		if (isOpenReadLine() && super.canRead())
			return pBufReader.readLine();
		else
			throw new IOException("File not opened - Can't read line in file '" + getAbsolutePath() + "'");
	}
	
	/**
	 * 16j_100
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<String>readLines() throws Exception{
		openReadLine();
		List<String> wLines = new ArrayList<String>();
		boolean wEof = false;
		String wLine;
		while (isOpenReadLine() && super.canRead() && !wEof){
			wLine = pBufReader.readLine();
			wEof = (wLine==null);
			if (!wEof)
				wLines.add(wLine );
		}
		close();
		return wLines;
	}
	/**
	 * @param aEncod
	 */
	public void setDefaultEncoding(String aEncod)
	{
		pDefaultEncoding = aEncod;
	}
	 /**
	 *  Sur un openWrite (non append) et si le fgichier existe on peut 
	 *  -> soit conserver le BOM du fichier existant
	 *  -> soit le forcer avec la valeur de l'encodage shouaite
	 *  @param aValue
	 *  --> pKeepExistingBOM=true conserve le BOM du fichier
	 *  --> pKeepExistingBOM=false force le BOM avec l'encodage souhaite
	 */
	public void setKeepExistingBOM(boolean aValue)
	{
		pKeepExistingBOM = aValue;
	}
	/**
	 * @param aCol
	 * @throws Exception
	 */
	public void write(Collection<Object> aCol) throws Exception
	{
		if (aCol != null)
		{
			checkWrite();
			Iterator<Object>  wIt = aCol.iterator();
			while (wIt.hasNext())
				writeLine(wIt.next().toString());
		}
	}
	/**
	 * @param aString
	 * @throws Exception
	 */
	public void write(String aString) throws Exception
	{
		checkWrite();
		pBufWriter.write(aString);
		pBufWriter.flush();
	}
	/**
	 * @param aStrBuf
	 * @throws Exception
	 */
	public void write(StringBuilder aStrBuf) throws Exception
	{
		write(aStrBuf.toString());
	}
	/**
	 * @param aCol
	 * @throws Exception
	 */
	public void writeAll(Collection<Object>  aCol) throws Exception
	{
		openWrite();
		write(aCol);
		close();
	}
	/**
	 * @param aString
	 * @throws Exception
	 */
	public void writeAll(String aString) throws Exception
	{
		openWrite();
		write(aString);
		close();
	}
	/**
	 * Ecrit l'encodage dasn le fichier
	 * @throws Exception
	 */
	private void writeEncoding() throws Exception
	{
		byte[] wBOM = null;
		if (pEncoding == null || pEncoding.equals(ENCODING_ISO_8859_1))
			wBOM = null;
		else if (pEncoding.equals(ENCODING_UTF_8))
			wBOM = BOM_UTF_8;
		if (pEncoding.equals(ENCODING_UTF_16BE))
			wBOM = BOM_UTF_16BE;
		else if (pEncoding.equals(ENCODING_UTF_16LE))
			wBOM = BOM_UTF_16LE;
		else if (pEncoding.equals(ENCODING_UTF_32BE))
			wBOM = BOM_UTF_32BE;
		else if (pEncoding.equals(ENCODING_UTF_32LE))
			wBOM = BOM_UTF_32LE;

		FileOutputStream wS = getOutputStream(false);
		if (wBOM != null)
			wS.write(wBOM);
		wS.flush();
		wS.close();
	}
	/**
	 * @throws Exception
	 */
	public void writeLine() throws Exception
	{
		writeLine((String) null);
	}
	/**
	 * @param aString
	 * @throws Exception
	 */
	public void writeLine(String aString) throws Exception
	{
		checkWrite();
		if (aString != null)
			pBufWriter.write(aString);
		pBufWriter.newLine();
		pBufWriter.flush();
	}
	/**
	 * @param aStrBuf
	 * @throws Exception
	 */
	public void writeLine(StringBuilder aStrBuf) throws Exception
	{
		writeLine(aStrBuf.toString());
	}
}
