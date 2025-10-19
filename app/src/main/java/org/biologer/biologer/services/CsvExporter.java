package org.biologer.biologer.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.sql.EntryDb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class for exporting local entries into CSV format.
 * All heavy lifting is handled here, keeping ActivityLanding clean.
 */
public class CsvExporter {

    private static final String TAG = "Biologer.CsvExporter";

    /**
     * Exports all entries from ObjectBox to a CSV file in external storage.
     * Runs synchronously — for large datasets, consider running this on a background thread.
     */
    public static void exportEntriesToCsv(Context context) {
        String filename = "Export_" + new SimpleDateFormat("yyMMddss", Locale.getDefault()).format(new Date());
        Uri uri = FileManipulation.newExternalDocumentFile(context, filename, ".csv");

        if (uri == null) {
            Toast.makeText(context, R.string.file_creation_failed, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Could not create URI for CSV export.");
            return;
        }

        try (OutputStream output = context.getContentResolver().openOutputStream(uri);
             CSVWriter writer = new CSVWriter(
                     new OutputStreamWriter(output),
                     CSVWriter.DEFAULT_SEPARATOR,
                     CSVWriter.DEFAULT_QUOTE_CHARACTER,
                     CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                     CSVWriter.DEFAULT_LINE_END
             )) {

            // Write headers
            writer.writeNext(new String[]{
                    context.getString(R.string.taxon),
                    context.getString(R.string.year),
                    context.getString(R.string.month),
                    context.getString(R.string.day),
                    context.getString(R.string.latitude),
                    context.getString(R.string.longitude),
                    context.getString(R.string.elevation),
                    context.getString(R.string.csv_accuracy),
                    context.getString(R.string.location),
                    context.getString(R.string.time),
                    context.getString(R.string.csv_note),
                    context.getString(R.string.found_dead),
                    context.getString(R.string.note_on_dead),
                    context.getString(R.string.observer),
                    context.getString(R.string.identifier),
                    context.getString(R.string.sex),
                    context.getString(R.string.number),
                    context.getString(R.string.project),
                    context.getString(R.string.habitat),
                    context.getString(R.string.found_on),
                    context.getString(R.string.stage),
                    context.getString(R.string.original_observation),
                    context.getString(R.string.dataset),
                    context.getString(R.string.data_license),
                    context.getString(R.string.image_license),
                    context.getString(R.string.atlas_code)
            });

            ArrayList<EntryDb> entries = (ArrayList<EntryDb>) App.get().getBoxStore().boxFor(EntryDb.class).getAll();
            String observer = ObjectBoxHelper.getUserName();

            for (EntryDb e : entries) {
                String[] row = {
                        e.getTaxonSuggestion(),
                        e.getYear(),
                        e.getMonth(),
                        e.getDay(),
                        formatDouble(e.getLattitude(), 6),
                        formatDouble(e.getLongitude(), 6),
                        formatDouble(e.getElevation(), 0),
                        formatDouble(e.getAccuracy(), 0),
                        e.getLocation(),
                        e.getTime(),
                        e.getComment(),
                        translateFoundDead(context, e.getDeadOrAlive()),
                        e.getCauseOfDeath(),
                        observer,
                        observer,
                        StageAndSexLocalization.getSexLocale(context, e.getSex()),
                        safeToString(e.getNoSpecimens()),
                        e.getProjectId(),
                        e.getHabitat(),
                        e.getFoundOn(),
                        StageAndSexLocalization.getStageLocaleFromID(context, e.getStage()),
                        e.getTaxonSuggestion(),
                        context.getString(R.string.dataset),
                        translateLicence(context, e.getDataLicence()),
                        translateLicence(context, String.valueOf(e.getImageLicence())),
                        safeToString(e.getAtlasCode())
                };
                writer.writeNext(row);
            }

            Toast.makeText(context, context.getString(R.string.export_to_csv_success) + " " + filename, Toast.LENGTH_LONG).show();
            Log.i(TAG, "CSV exported successfully: " + filename);

        } catch (FileNotFoundException e) {
            Toast.makeText(context, context.getString(R.string.file_not_found) + " " + e, Toast.LENGTH_LONG).show();
            Log.e(TAG, "File not found: " + e);
        } catch (IOException e) {
            Toast.makeText(context, context.getString(R.string.io_error) + " " + e, Toast.LENGTH_LONG).show();
            Log.e(TAG, "IO Error: " + e);
        }
    }

    private static String formatDouble(Double d, int decimals) {
        if (d == null) return "";
        String format = "%." + decimals + "f";
        return String.format(Locale.ENGLISH, format, d);
    }

    private static String safeToString(Object o) {
        return (o == null) ? "" : o.toString();
    }

    private static String translateLicence(Context context, String code) {
        return switch (code) {
            case "10" -> context.getString(R.string.export_licence_10);
            case "11" -> context.getString(R.string.export_licence_11);
            case "20" -> context.getString(R.string.export_licence_20);
            case "30" -> context.getString(R.string.export_licence_30);
            case "40" -> context.getString(R.string.export_licence_40);
            default -> "";
        };
    }

    private static String translateFoundDead(Context context, String alive) {
        if (alive == null) return "";
        return alive.equals("true")
                ? context.getString(R.string.no)
                : context.getString(R.string.yes);
    }
}
