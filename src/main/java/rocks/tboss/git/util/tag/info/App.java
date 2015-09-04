package rocks.tboss.git.util.tag.info;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Mini app to walk history from a tag to see what other tags are in the history.
 */
public class App {
    public static void main(String[] args) throws IOException, GitAPIException {
        final List<String> arguments = Arrays.asList(args);
        doRealWork(System.out, arguments);
    }

    private static void doRealWork(PrintStream out, List<String> arguments) throws IOException, GitAPIException {
        if (arguments.isEmpty()) {
            out.println("Usage: PrintTagHistory <path> <tag> <depth> ");
            out.println("<path> file path of git repo e.g. C:/dev/my-project");
            out.println("<tag> name of tag to check history for e.g. 1.0");
            out.println("<depth> (optional) number of tags to check in history - default is 10");
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

        printTagHistory(tagName, path, depth, out);
    }

    public static void printTagHistory(final String tagName, final String path, final int depth, PrintStream out) throws IOException, GitAPIException {
        out.println("Analysing git repository at " + path);
        final FileRepositoryBuilder builder = new FileRepositoryBuilder();
        final Repository repository = builder.setGitDir(new File(path))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .setMustExist(true)
                .build();
        final List<Ref> tags = new Git(repository).tagList().call();

        final RevWalk revWalk = new ObjectWalk(repository);

        final Ref base = repository.getTags().get(tagName);
        out.println("Finding ancestor branches of " + readableTagName(base) + " (to a max depth of " + depth + " tags in the past)");
        out.println("");
        final ObjectId rootId = base.getObjectId();
        final RevCommit root = revWalk.parseCommit(rootId);
        revWalk.markStart(root);
        int count = 0;
        final Iterator<RevCommit> commits = revWalk.iterator();
        while (commits.hasNext()) {
            final RevCommit next =  commits.next();
            for (final Ref tag : tags) {
                if (!tag.getName().equals(base.getName()) && tag.getObjectId().equals(next.getId())) {
                    final BranchComparison branchComparison = calculateDivergence(repository, base, tag);
                    out.println("Includes changes from: " + readableTagName(tag));
                    out.println("  - Ahead: " + branchComparison.ahead + " commits");
                    if (branchComparison.behind>0) {
                        out.println("  - Behind: " + branchComparison.behind + " (this is really bad!!! should always be ahead, never behind)");
                    }
                    count++;
                    if (count>=depth) {
                        return;
                    }
                }
            }
        }
    }

    private static String readableTagName(final Ref tag) {
        final String tagNamePrefix = "refs/tags/";
        final String name = tag.getName();
        if (!name.startsWith(tagNamePrefix)) {
            return name;
        }
        return name.substring(tagNamePrefix.length());
    }

    private static BranchComparison calculateDivergence(final Repository repository, final Ref local, final Ref tracking ) throws IOException {
        final RevWalk walk = new RevWalk( repository );
        final BranchComparison result = new BranchComparison();
        try {
            final RevCommit localCommit = walk.parseCommit( local.getObjectId() );
            final RevCommit trackingCommit = walk.parseCommit( tracking.getObjectId() );
            walk.setRevFilter( RevFilter.MERGE_BASE );
            walk.markStart( localCommit );
            walk.markStart( trackingCommit );
            final RevCommit mergeBase = walk.next();
            walk.reset();
            walk.setRevFilter( RevFilter.ALL );
            result.ahead = RevWalkUtils.count( walk, localCommit, mergeBase );
            result.behind = RevWalkUtils.count( walk, trackingCommit, mergeBase );
            return result;
        } finally {
            walk.dispose();
        }
    }
}
