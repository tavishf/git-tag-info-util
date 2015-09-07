package rocks.tboss.git.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class GitUtil {

    public static final String TAG_NAME_PREFIX = "refs/tags/";

    /**
     * Starting at tag with name tagName, go back in its history and print out any commits that also have tags.
     *
     * @param tagName name of tag to find history for
     * @param path path to git repo on disk
     * @param depth max number of tags in history to print
     * @param out where to print the info
     * @throws IOException
     * @throws GitAPIException
     */
    static void printTagHistory(final String tagName, final String path, final int depth, final PrintStream out) throws IOException, GitAPIException {
        out.println("Analysing git repository at " + path);
        final Repository repository = loadRepository(path);

        final Ref base = repository.getTags().get(tagName);
        if (null==base) {
            out.println("Couldn't find a tag named "+tagName);
            return;
        }
        out.println("Finding ancestor tags of " + readableTagName(base) + " (to a max depth of " + depth + " tags in the past)");
        out.println("");

        final List<BranchComparison> branchComparisons = getAncestorComparisons(repository, base);
        printAncestors(out, base, branchComparisons, depth);
    }

    static void printBranchComparison(final String base, final String other, final String path, PrintStream out) throws IOException {
        out.println("Analysing git repository at " + path);
        final Repository repository = loadRepository(path);

        out.println();
        out.println("Comparing " + base + " with " + other);
        final Ref baseRef = ensureRef(base, repository);
        final Ref otherRef = ensureRef(other, repository);

        out.println();
        final BranchComparison branchComparison = calculateDivergence(repository, baseRef, otherRef);
        out.println(branchComparison.base.getName() + " is " + branchComparison.ahead + " commits ahead of "+branchComparison.other.getName());
        out.println(branchComparison.base.getName() + " is " + branchComparison.behind + " commits behind " + branchComparison.other.getName());
    }

    private static Ref ensureRef(final String name, final Repository repository) throws IOException {
        final Ref ref = repository.getRef(name);
        if (null==ref) {
            throw new IllegalArgumentException("Couldn't find "+name);
        }
        return ref;
    }

    private static List<BranchComparison> getAncestorComparisons(Repository repository, Ref base) throws IOException, GitAPIException {
        final RevWalk revWalk = new ObjectWalk(repository);
        final ObjectId rootId = base.getObjectId();
        final RevCommit root = revWalk.parseCommit(rootId);
        revWalk.markStart(root);

        final List<Ref> tags = new Git(repository).tagList().call();
        final Map<ObjectId, Ref> tagsByCommitId = tagsByObjectId(tags, repository);
        return getRevCommitStream(revWalk)
                //only where it points to a tag
                .filter(commit -> null != tagsByCommitId.get(commit.getId()))
                .map(commit -> {
                    final Ref tag = tagsByCommitId.get(commit.getId());
                    return calculateDivergence(repository, base, tag);
                })
                .collect(Collectors.toList());
    }

    private static void printAncestors(final PrintStream out, final Ref base, final List<BranchComparison> branchComparisons, final int depth) {
        int count = 0;
        for (final BranchComparison branchComparison : branchComparisons) {
            final Ref tag = branchComparison.other;
            if (!tag.getName().equals(base.getName())) {
                out.println("Includes changes from: " + readableTagName(branchComparison.other));
                out.println("  - Ahead: " + branchComparison.ahead + " commits");
                if (branchComparison.behind > 0) {
                    //Pretty sure this is actually impossible?
                    out.println("  - Behind: " + branchComparison.behind + " (this is really bad!!! should always be ahead, never behind)");
                }
                count++;
                if (count>=depth) {
                    return;
                }
            }
        }
    }

    private static Stream<RevCommit> getRevCommitStream(final RevWalk revWalk) {
        final Iterator<RevCommit> commits = revWalk.iterator();
        final Iterable<RevCommit> iterable = () -> commits;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    private static Repository loadRepository(String path) throws IOException {
        final FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setGitDir(new File(path))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .setMustExist(true)
                .build();
    }

    private static Map<ObjectId, Ref> tagsByObjectId(List<Ref> tags, Repository repository) {
        final HashMap<ObjectId, Ref> result = new HashMap<>();
        for (final Ref tag : tags) {
            final ObjectId key;
            final Ref peeledRef = repository.peel(tag);
            if (peeledRef.getPeeledObjectId() != null) {
                key = peeledRef.getPeeledObjectId();
            } else {
                key = tag.getObjectId();
            }
            result.put(key, tag);
        }
        return result;
    }

    private static String readableTagName(final Ref tag) {
        final String name = tag.getName();
        if (!name.startsWith(TAG_NAME_PREFIX)) {
            return name;
        }
        return name.substring(TAG_NAME_PREFIX.length());
    }

    private static BranchComparison calculateDivergence(final Repository repository, final Ref base, final Ref other) {
        final RevWalk walk = new RevWalk(repository);
        try {
            //Adapted from elsewhere, don't fully understand how the RevWalk stuff works
            final RevCommit baseCommit;
            baseCommit = walk.parseCommit(base.getObjectId());
            final RevCommit compareCommit = walk.parseCommit(other.getObjectId());
            walk.setRevFilter(RevFilter.MERGE_BASE);
            walk.markStart(baseCommit);
            walk.markStart(compareCommit);
            final RevCommit mergeBase = walk.next();
            walk.reset();
            walk.setRevFilter(RevFilter.ALL);
            int ahead = RevWalkUtils.count(walk, baseCommit, mergeBase);
            int behind = RevWalkUtils.count(walk, compareCommit, mergeBase);
            return new BranchComparison(base, other, behind, ahead);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            walk.dispose();
        }
    }
}
