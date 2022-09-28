package studies

import kotlinx.coroutines.flow.Flow
import ui.common.table.TableSchema

interface Study {

    val name: String

    val provider: Provider

    interface Provider {

        val data: Flow<List<Model>>

        val schema: TableSchema<Model>
    }

    interface Model
}
