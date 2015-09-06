package rocks.tboss.git.util.tag.info;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * App class to parse commandline args and call methods to do real work as appropriate
 */
public class PrintTagHistory {
    public static void main(String[] args) throws IOException, GitAPIException {
        final List<String> arguments = Arrays.asList(args);
        if (arguments.size() < 2) {
            System.out.println("Usage: PrintTagHistory <path> <tag> <depth> ");
            System.out.println("<path> file path of git repo e.g. C:/dev/my-project");
            System.out.println("<tag> name of tag to check history for e.g. 1.0");
            System.out.println("<depth> (optional) number of tags to check in history - default is 10");
            return;
        }
        String path = arguments.get(0);
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        if (!path.endsWith(".git")) {
            path = path + ".git";
        }
        final String tagName = arguments.get(1);
        final int depth = arguments.size()>2 ? Integer.parseInt(arguments.get(2)) : 10;

        GitUtil.printTagHistory(tagName, path, depth, System.out);
    }

}
