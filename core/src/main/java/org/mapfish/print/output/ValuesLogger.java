package org.mapfish.print.output;

import com.google.common.collect.Maps;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.mapfish.print.PrintException;
import org.mapfish.print.config.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Class for logging the values in a {@link org.mapfish.print.output.Values} object.
 *
 * @author Jesse on 2/26/2015.
 */
public final class ValuesLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValuesLogger.class);
    private static final int STANDARD_INDENT_SIZE = 4;
    private int indent = STANDARD_INDENT_SIZE;
    private final StringBuilder builder = new StringBuilder();

    private ValuesLogger() {
        // no public constructor
    }


    /**
     * Log the values for the provided template.
     *
     * @param templateName the name of the template the values came from
     * @param template     the template object
     * @param values       the resultant values
     */
    public static void log(final String templateName, final Template template, final Values values) {
        new ValuesLogger().doLog(templateName, template, values);
    }

    private void doLog(final String templateName, final Template template, final Values values) {
        if (!LOGGER.isInfoEnabled()) {
            return;
        }

        this.builder.append("\nThis log message details the parameters available for use in the Jasper templates ");
        this.builder.append("for\n  Mapfish Template: ").append(templateName).append("\n");
        this.builder.append("  Jasper Template name: ").append(template.getReportTemplate()).append('\n');
        this.builder.append("  The following parameters are available for use in the templates: \n");
        for (Map.Entry<String, Object> parameter : values.asMap().entrySet()) {
            boolean isTableDataKey = parameter.getKey().equals(template.getTableDataKey());
            logValue(templateName, parameter, isTableDataKey);
        }

        LOGGER.info(this.builder.toString());
    }

    private void logValue(final String templateName, final Map.Entry<String, ?> parameter, final boolean isTableDataKey) {
        addIndent().append("* ").append(parameter.getKey());
        if (isTableDataKey) {
            if (parameter.getValue() instanceof JRDataSource) {
                this.builder.append(" <tableDataKey>");
            } else {
                final String message = "The output value: '" + parameter.getKey() + "' is defined in the template: '" +
                                       templateName + "' as the tableDataKey but is not a '" + JRDataSource.class.getName() +
                                       "' object as was expected.  Instead it is " + parameter.getValue().getClass().getName();
                throw new PrintException(message);
            }
        }

        this.builder.append(" (").append(parameter.getValue().getClass().getName()).append(")\n");
        if (parameter.getValue() instanceof JRDataSource) {
            String section = isTableDataKey ? "'Top-level' template" : "sub-template";

            if (!isTableDataKey) {
                addIndent().append("  - This value is a Jasper Reports DataSource and thus can be passed to a ").
                        append("subtemplate as a DataSource and used in the subtemplate's detail band.\n");
            }
            if (parameter.getValue() instanceof JRMapCollectionDataSource) {
                JRMapCollectionDataSource source = (JRMapCollectionDataSource) parameter.getValue();

                addIndent().append("  - This DataSource contains the "
                                   + "following columns (All rows are analyzed for their columns, thus each row may "
                                   + "only have a subset of the columns)\n");

                this.indent += STANDARD_INDENT_SIZE;
                Map<String, Object> columns = Maps.newHashMap();

                for (Map<String, ?> row : source.getData()) {
                    for (Map.Entry<String, ?> column : row.entrySet()) {
                        columns.put(column.getKey(), column.getValue());
                    }
                }

                for (Map.Entry<String, ?> column : columns.entrySet()) {
                    logValue(templateName, column, false);
                }
                this.indent -= STANDARD_INDENT_SIZE;
            } else {
                addIndent().append("  - This datasource is not a type that can be introspected but it can be used in"
                                   + " a detail section of ").
                        append(section).append(" if the structure is known.\n");

            }
        }
    }

    private StringBuilder addIndent() {
        for (int i = 0; i < this.indent; i++) {
            this.builder.append(" ");
        }
        return this.builder;
    }

}
