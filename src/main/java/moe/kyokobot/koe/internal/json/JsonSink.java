package moe.kyokobot.koe.internal.json;

import java.util.Collection;
import java.util.Map;

/**
 * Common interface for {@link JsonAppendableWriter}, {@link JsonStringWriter} and {@link JsonBuilder}.
 * 
 * @param <S>
 *            A subclass of {@link JsonSink}.
 */
public interface JsonSink<S extends JsonSink<S>> {
	/**
	 * Emits the start of an array.
	 */
	S array(Collection<?> c);

	/**
	 * Emits the start of an array with a key.
	 */
	S array(String key, Collection<?> c);
	
	/**
	 * Emits the start of an object.
	 */
	S object(Map<?, ?> map);

	/**
	 * Emits the start of an object with a key.
	 */
	S object(String key, Map<?, ?> map);

	/**
	 * Emits a 'null' token.
	 */
	S nul();

	/**
	 * Emits a 'null' token with a key.
	 */
	S nul(String key);

	/**
	 * Emits an object if it is a JSON-compatible type, otherwise throws an exception.
	 */
	S value(Object o);

	/**
	 * Emits an object with a key if it is a JSON-compatible type, otherwise throws an exception.
	 */
	S value(String key, Object o);

	/**
	 * Emits a string value (or null).
	 */
	S value(String s);

	/**
	 * Emits an integer value.
	 */
	S value(int i);

	/**
	 * Emits a long value.
	 */
	S value(long l);

	/**
	 * Emits a boolean value.
	 */
	S value(boolean b);

	/**
	 * Emits a double value.
	 */
	S value(double d);

	/**
	 * Emits a float value.
	 */
	S value(float f);

	/**
	 * Emits a {@link Number} value.
	 */
	S value(Number n);

	/**
	 * Emits a string value (or null) with a key.
	 */
	S value(String key, String s);

	/**
	 * Emits an integer value with a key.
	 */
	S value(String key, int i);
	
	/**
	 * Emits a long value with a key.
	 */
	S value(String key, long l);

	/**
	 * Emits a boolean value with a key.
	 */
	S value(String key, boolean b);

	/**
	 * Emits a double value with a key.
	 */
	S value(String key, double d);

	/**
	 * Emits a float value with a key.
	 */
	S value(String key, float f);

	/**
	 * Emits a {@link Number} value with a key.
	 */
	S value(String key, Number n);

	/**
	 * Starts an array.
	 */
	S array();

	/**
	 * Starts an object.
	 */
	S object();

	/**
	 * Starts an array within an object, prefixed with a key.
	 */
	S array(String key);

	/**
	 * Starts an object within an object, prefixed with a key.
	 */
	S object(String key);

	/**
	 * Ends the current array or object.
	 */
	S end();
}
