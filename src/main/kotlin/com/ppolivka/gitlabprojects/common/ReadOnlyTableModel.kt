package com.ppolivka.gitlabprojects.common

import javax.swing.table.DefaultTableModel

/**
 * JTable table model that is not supporting editing of cells in table
 *
 * @author ppolivka
 * @since 22.12.2015
 */
class ReadOnlyTableModel(data: Array<Array<Any?>>, columnNames: Array<String>) :
    DefaultTableModel(data, columnNames) {

    override fun isCellEditable(row: Int, column: Int): Boolean {
        return false
    }
}