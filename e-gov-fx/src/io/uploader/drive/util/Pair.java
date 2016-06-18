
package io.uploader.drive.util;


public class Pair<T1, T2>
{
	private final T1 first ;
	private final T2 second ;
	
	private Pair(T1 l, T2 o) {
		super();
		this.first = l;
		this.second = o;
	}
	
	public T1 getFirst() {
		return first;
	}
	
	public T2 getSecond() {
		return second;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result
				+ ((second == null) ? 0 : second.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair<?, ?> other = (Pair<?, ?>) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}
	
	public static <T1, T2> Pair<T1, T2> newPair (T1 first, T2 second)
	{
		return new Pair<T1, T2> (first, second) ;
	}
}
