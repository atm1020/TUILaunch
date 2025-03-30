package com.github.atm1020.tuilaunchmodel

import com.github.atm1020.tuilaunch.model.TuiAppConfig
import javax.swing.table.AbstractTableModel

class TuiAppTableModel(private val apps: MutableList<TuiAppConfig>) : AbstractTableModel() {
    private val  actionId  = "ActionId"
    private val columnNames = arrayOf("Name", "Command", "Options", actionId)

    override fun getRowCount(): Int = apps.size
    override fun getColumnCount(): Int = columnNames.size
    override fun getColumnName(column: Int): String = columnNames[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        return when (columnIndex) {
            0 -> apps[rowIndex].name
            1 -> apps[rowIndex].command
            2 -> apps[rowIndex].options
            3 -> "TUILauncher.${apps[rowIndex].command}"
            else -> ""
        }
    }


    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return this.columnNames[columnIndex] != actionId
    }

    override fun setValueAt(aValue: Any, rowIndex: Int, columnIndex: Int) {
        val value = aValue.toString()
        when (columnIndex) {
            0 -> apps[rowIndex].name = value
            1 -> apps[rowIndex].command = value
            2 -> apps[rowIndex].options = value
            3 -> apps[rowIndex].description = value
        }
        fireTableCellUpdated(rowIndex, columnIndex)
    }

    fun addRow(app: TuiAppConfig) {
        apps.add(app)
        fireTableRowsInserted(apps.size - 1, apps.size - 1)
    }

    fun removeRow(rowIndex: Int) {
        if (rowIndex >= 0 && rowIndex < apps.size) {
            apps.removeAt(rowIndex)
            fireTableRowsDeleted(rowIndex, rowIndex)
        }
    }
}
