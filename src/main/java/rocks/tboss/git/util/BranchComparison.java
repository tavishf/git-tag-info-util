package rocks.tboss.git.util;

import org.eclipse.jgit.lib.Ref;

public class BranchComparison {
  final Ref base;
  final Ref other;
  final int behind;
  final int ahead;

  public BranchComparison(final Ref base, final Ref other, final int behind, final int ahead) {
    this.base = base;
    this.other = other;
    this.behind = behind;
    this.ahead = ahead;
  }
}
