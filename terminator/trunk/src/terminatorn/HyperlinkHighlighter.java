package terminatorn;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;
import e.util.*;

/**

@author Phil Norman
*/

public class HyperlinkHighlighter implements Highlighter {
	/** The underlined blue standard hyperlink style. */
	private final Style style = new Style(Color.blue, null, null, Boolean.TRUE);

	private static class HyperLinker {
		Pattern pattern;
		int relevantGroup;
		String command;
		boolean runInTab;
		
		/**
		 * We need a regular expression to match with, the index of the
		 * group within the expression that should be highlighted (0 if
		 * you want the whole expression highlighted, and the command
		 * to be run when the link is followed.
		 */
		HyperLinker(String regularExpression, int relevantGroup, String command, boolean runInTab) {
			this.pattern = Pattern.compile(regularExpression);
			this.relevantGroup = relevantGroup;
			this.command = command;
			this.runInTab = runInTab;
		}
		
		Matcher matcher(String line) {
			return pattern.matcher(line);
		}
		
		String command(Matcher matcher) {
			return matcher.replaceFirst(command);
		}
		
		boolean runInTab() {
			return runInTab;
		}
	}
	
	private ArrayList linkers = new ArrayList();
	{
		String[] linkerNames = Options.getSharedInstance().getPropertySetNames("Highlighter");
		for (int i = 0; i < linkerNames.length; i++) {
			Properties info = Options.getSharedInstance().getPropertySet("Highlighter", linkerNames[i]);
			String regexp = info.getProperty("regexp");
			if (regexp == null) {
				Log.warn("Missing regexp in highlighter definition " + linkerNames[i] + ".");
				continue;
			}
			String openInTabString = info.getProperty("openInTab", "false");
			boolean openInTab = openInTabString.equalsIgnoreCase("yes") || openInTabString.equalsIgnoreCase("true");
			int highlightGroup;
			try {
				highlightGroup = Integer.parseInt(info.getProperty("highlightGroup", "1"));
			} catch (NumberFormatException ex) {
				Log.warn("Invalid highlightGroup in highlighter definition " + linkerNames[i] + " (number expected).");
				continue;
			}
			String command = info.getProperty("command");
			if (command == null) {
				Log.warn("Missing command in highlighter definition " + linkerNames[i] + ".");
				continue;
			}
			linkers.add(new HyperLinker(regexp, highlightGroup, command, openInTab));
		}
	}

	public String getName() {
		return "Hyperlink Highlighter";
	}
	
	/** Request to add highlights to all lines of the view from the index given onwards. */
	public void addHighlights(JTextBuffer view, int firstLineIndex) {
		TextBuffer model = view.getModel();
		for (int i = firstLineIndex; i < model.getLineCount(); i++) {
			String line = model.getLine(i);
			addHighlights(view, i, line);
		}
	}
	
	private void addHighlights(JTextBuffer view, int lineIndex, String text) {
		for (int i = 0; i < linkers.size(); ++i) {
			HyperLinker linker = (HyperLinker) linkers.get(i);
			Matcher matcher = linker.matcher(text);
			while (matcher.find()) {
				Location start = new Location(lineIndex, matcher.start(linker.relevantGroup));
				Location end = new Location(lineIndex, matcher.end(linker.relevantGroup));
				Highlight highlight = new Highlight(HyperlinkHighlighter.this, start, end, style);
				highlight.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				view.addHighlight(highlight);
			}
		}
	}

	/** Request to do something when the user clicks on a Highlight generated by this Highlighter. */
	public void highlightClicked(JTextBuffer view, Highlight highlight, String text, MouseEvent event) {
		for (int i = 0; i < linkers.size(); ++i) {
			HyperLinker linker = (HyperLinker) linkers.get(i);
			Matcher matcher = linker.matcher(text);
			while (matcher.find()) {
				String command = linker.command(matcher);
				if (linker.runInTab()) {
					view.getController().openCommandPane(command, true);
				} else {
					try {
						Runtime.getRuntime().exec(command);
					} catch (Exception ex) {
						Log.warn("Couldn't show '" + text + "' (with '" + linker.command(matcher) + "')", ex);
					}
				}
			}
		}
	}
}
