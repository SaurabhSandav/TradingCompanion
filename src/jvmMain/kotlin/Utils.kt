import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

internal inline fun <T, R> Flow<List<T>>.mapList(
    crossinline transform: suspend (value: T) -> R,
): Flow<List<R>> = transform { list ->
    return@transform emit(list.map { transform(it) })
}
