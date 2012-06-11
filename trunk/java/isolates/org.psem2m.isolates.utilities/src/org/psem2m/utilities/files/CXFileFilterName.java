package org.psem2m.utilities.files;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Iterator;

import org.psem2m.utilities.CXListUtils;
import org.psem2m.utilities.CXStringUtils;

/**
 * 16j_102 - mise en place de la classe CXFileFilterName
 * 
 * @author ogattaz
 * 
 */
public class CXFileFilterName extends CXFileFilter implements FileFilter {

	private HashSet<String> pListRegExp = new HashSet<String>();

	@Override
	public String toString() {
		String[] wStrs = pListRegExp.toArray(new String[0]);
		StringBuilder wSB = new StringBuilder();
		wSB.append(String.format("FilterName(%s)=[%s]", includer(), CXStringUtils.stringTableToString(wStrs)));
		if (hasSubFileFilter())
			wSB.append(SEPARATOR).append(getSubFileFilter().toString());
		return wSB.toString();
	}

	public CXFileFilterName(String aListRegExp) {
		this(aListRegExp, null, INCLUDE);
	}

	public CXFileFilterName(String aListRegExp, FileFilter aSubFileFilter, boolean aInclude) {
		super(aSubFileFilter, aInclude);
		if (aListRegExp != null) {
			CXListUtils.loadStrCollection(pListRegExp, aListRegExp, SEPARATOR);
		}
	}

	@Override
	public boolean accept(File pathname) {
		if (pathname.isDirectory())
			return true;

		boolean wRes = !include();
		String wName = new CXFile(pathname).getNameWithoutExtension();

		Iterator<String> wRegExps = pListRegExp.iterator();
		String wRegExp;
		while (wRegExps.hasNext()) {
			wRegExp = wRegExps.next();
			// si subpath contenu dans AbsolutePath :

			if (wName.matches(wRegExp)) {
				wRes = include();
				break;
			}
		}
		if (wRes && hasSubFileFilter())
			wRes = getSubFileFilter().accept(pathname);

		return wRes;
	}

}