/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2019, Sualeh Fatehi <sualeh@hotmail.com>.
All rights reserved.
------------------------------------------------------------------------

SchemaCrawler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

SchemaCrawler and the accompanying materials are made available under
the terms of the Eclipse Public License v1.0, GNU General Public License
v3 or GNU Lesser General Public License v3.

You may elect to redistribute this code under any of these licenses.

The Eclipse Public License is available at:
http://www.eclipse.org/legal/epl-v10.html

The GNU General Public License v3 and the GNU Lesser General Public
License v3 are available at:
http://www.gnu.org/licenses/

========================================================================
*/

package schemacrawler.test;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static schemacrawler.test.utility.ExecutableTestUtility.executableExecution;
import static schemacrawler.test.utility.ExecutableTestUtility.hasSameContentAndTypeAs;
import static schemacrawler.test.utility.FileHasContent.classpathResource;
import static schemacrawler.test.utility.FileHasContent.hasSameContentAs;
import static schemacrawler.test.utility.FileHasContent.outputOf;
import static schemacrawler.test.utility.TestUtility.clean;

import java.sql.Connection;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import schemacrawler.schemacrawler.Config;
import schemacrawler.schemacrawler.InfoLevel;
import schemacrawler.schemacrawler.RegularExpressionInclusionRule;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.test.utility.TestDatabaseConnectionParameterResolver;
import schemacrawler.tools.executable.SchemaCrawlerExecutable;
import schemacrawler.tools.options.OutputFormat;
import schemacrawler.tools.options.TextOutputFormat;
import schemacrawler.tools.text.operation.Operation;
import schemacrawler.tools.text.schema.SchemaTextDetailType;
import schemacrawler.tools.text.schema.SchemaTextOptionsBuilder;

@ExtendWith(TestDatabaseConnectionParameterResolver.class)
public class LintOutputTest
{

  private static final String TEXT_OUTPUT = "lint_text_output/";
  private static final String COMPOSITE_OUTPUT = "lint_composite_output/";
  private static final String JSON_OUTPUT = "lint_json_output/";

  @Test
  public void compareCompositeOutput(final Connection connection)
    throws Exception
  {
    clean(COMPOSITE_OUTPUT);

    final String queryCommand1 = "dump_top5";
    final Config queriesConfig = new Config();
    queriesConfig
      .put(queryCommand1,
           "SELECT TOP 5 ${orderbycolumns} FROM ${table} ORDER BY ${orderbycolumns}");

    final String[] commands = new String[] {
                                             SchemaTextDetailType.brief
                                             + "," + Operation.count + ","
                                             + "lint",
                                             queryCommand1 + ","
                                                       + SchemaTextDetailType.brief
                                                       + "," + "lint", };

    final SchemaCrawlerOptionsBuilder schemaCrawlerOptionsBuilder = SchemaCrawlerOptionsBuilder
      .builder()
      .includeSchemas(new RegularExpressionInclusionRule(".*FOR_LINT"))
      .withSchemaInfoLevel(SchemaInfoLevelBuilder.maximum());
    final SchemaCrawlerOptions schemaCrawlerOptions = schemaCrawlerOptionsBuilder
      .toOptions();

    assertAll(outputFormats()
      .flatMap(outputFormat -> Arrays.stream(commands).map(command -> () -> {

        final String referenceFile = command + "." + outputFormat.getFormat();

        final SchemaTextOptionsBuilder schemaTextOptionsBuilder = SchemaTextOptionsBuilder
          .builder();
        schemaTextOptionsBuilder.noInfo(false);
        queriesConfig.putAll(schemaTextOptionsBuilder.toConfig());

        final SchemaCrawlerExecutable executable = new SchemaCrawlerExecutable(command);
        executable.setSchemaCrawlerOptions(schemaCrawlerOptions);
        executable.setAdditionalConfiguration(queriesConfig);

        assertThat(outputOf(executableExecution(connection,
                                                executable,
                                                outputFormat)),
                   hasSameContentAndTypeAs(classpathResource(COMPOSITE_OUTPUT
                                                             + referenceFile),
                                           outputFormat));

      })));
  }

  @Test
  public void compareJsonOutput(final Connection connection)
    throws Exception
  {
    clean(JSON_OUTPUT);

    final InfoLevel infoLevel = InfoLevel.standard;
    final OutputFormat outputFormat = TextOutputFormat.json;

    final SchemaCrawlerOptionsBuilder schemaCrawlerOptionsBuilder = SchemaCrawlerOptionsBuilder
      .builder()
      .includeSchemas(new RegularExpressionInclusionRule(".*FOR_LINT"))
      .withSchemaInfoLevel(infoLevel.toSchemaInfoLevel());
    final SchemaCrawlerOptions schemaCrawlerOptions = schemaCrawlerOptionsBuilder
      .toOptions();

    final SchemaCrawlerExecutable executable = new SchemaCrawlerExecutable("lint");
    executable.setSchemaCrawlerOptions(schemaCrawlerOptions);

    assertThat(outputOf(executableExecution(connection,
                                            executable,
                                            outputFormat)),
               hasSameContentAndTypeAs(classpathResource(JSON_OUTPUT
                                                         + "lints.json"),
                                       outputFormat));
  }

  @Test
  public void compareTextOutput(final Connection connection)
    throws Exception
  {
    clean(TEXT_OUTPUT);

    final InfoLevel infoLevel = InfoLevel.standard;

    final SchemaCrawlerOptionsBuilder schemaCrawlerOptionsBuilder = SchemaCrawlerOptionsBuilder
      .builder()
      .includeSchemas(new RegularExpressionInclusionRule(".*FOR_LINT"))
      .withSchemaInfoLevel(infoLevel.toSchemaInfoLevel());
    final SchemaCrawlerOptions schemaCrawlerOptions = schemaCrawlerOptionsBuilder
      .toOptions();

    final SchemaCrawlerExecutable executable = new SchemaCrawlerExecutable("lint");
    executable.setSchemaCrawlerOptions(schemaCrawlerOptions);

    assertThat(outputOf(executableExecution(connection, executable)),
               hasSameContentAs(classpathResource(TEXT_OUTPUT + "lint.txt")));
  }

  private Stream<TextOutputFormat> outputFormats()
  {
    return Arrays.stream(new TextOutputFormat[] {
                                                  TextOutputFormat.text,
                                                  TextOutputFormat.html,
                                                  TextOutputFormat.json });
  }

}
