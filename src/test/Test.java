package test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {
	public static void main(String... args) {
		String contex = ".+第(\\d+)集";
//		Pattern p = Pattern.compile("\\*+(第(\\d+)集)$");
		Pattern p = Pattern.compile(contex);
		String text = "绝对达令(大结局)第40集xxx";
		Matcher match = p.matcher(text);
		if (match.find()) {
			int times = match.groupCount();
			for (int i=0;i<=times;i++) {
				System.out.println("group : " + i + " : " + match.group(i));
			}
		}
	}
}
