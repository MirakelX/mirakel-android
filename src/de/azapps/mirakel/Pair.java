package de.azapps.mirakel;

public class Pair<F,S> {

	  private final F left;
	  private final S right;

	  public Pair(F left, S right) {
	    this.left = left;
	    this.right = right;
	  }

	  public F getLeft() { return left; }
	  public S getRight() { return right; }

	  @Override
	  public int hashCode() { return left.hashCode() ^ right.hashCode(); }

	  @Override
	  public boolean equals(Object o) {
	    if (o == null) return false;
	    if (!(o instanceof Pair)) return false;
	    Pair<F,S> pairo = (Pair) o;
	    return this.left.equals(pairo.getLeft()) &&
	           this.right.equals(pairo.getRight());
	  }

	}
