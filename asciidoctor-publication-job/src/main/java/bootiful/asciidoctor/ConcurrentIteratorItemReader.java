package bootiful.asciidoctor;

import org.springframework.batch.item.support.IteratorItemReader;

/**
 * This is important because the reads happen in a
 * {@link org.springframework.batch.core.Step} with a
 * {@link org.springframework.core.task.TaskExecutor} configured on them. The
 * out-of-the-box {@link IteratorItemReader} doesn't seem to be thread-safe.
 */
class ConcurrentIteratorItemReader<T> extends IteratorItemReader<T> {

	private final Object monitor = new Object();

	ConcurrentIteratorItemReader(Iterable<T> iterable) {
		super(iterable);
	}

	@Override
	public T read() {
		synchronized (this.monitor) {
			return super.read();
		}
	}

}
