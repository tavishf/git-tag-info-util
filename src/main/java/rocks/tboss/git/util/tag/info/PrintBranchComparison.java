package rocks.tboss.git.util.tag.info;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * App class to parse commandline args and call methods to do real work as appropriate
 */
public class PrintBranchComparison {
    public static void main(String[] args) throws IOException, GitAPIException {
        final List<String> arguments = Arrays.asList(args);
        if (arguments.size() != 3) {
            System.out.println("Usage: PrintBranchComparison <path> <base> <other> ");
            System.out.println("<path> file path of git repo e.g. C:/dev/my-project");
            System.out.println("<base> name of base branch (or tag)");
            System.out.println("<other> name of other branch (or tag)");
            return;
        }
        String path = arguments.get(0);
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        if (!path.endsWith(".git")) {
            path = path + ".git";
        }
        final String base = arguments.get(1);
        final String other = arguments.get(2);

        GitUtil.printBranchComparison(base, other, path, System.out);
    }

}
