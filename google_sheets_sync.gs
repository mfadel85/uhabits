/**
 * Google Apps Script for uHabits Data Integration
 * 
 * This script fetches data from your uHabits API and populates Google Sheets.
 * Once in Sheets, you can easily connect to Google Looker Studio.
 * 
 * SETUP INSTRUCTIONS:
 * 1. Open Google Sheets
 * 2. Extensions → Apps Script
 * 3. Paste this code
 * 4. Save and run `setupSheets()`
 * 5. Set up time-based trigger for `refreshAllData()`
 */

// Configuration
const API_BASE_URL = 'https://kpitracker.quest/api/looker.php';

/**
 * Create menu in Google Sheets for easy access
 */
function onOpen() {
  const ui = SpreadsheetApp.getUi();
  ui.createMenu('uHabits Sync')
    .addItem('Refresh All Data', 'refreshAllData')
    .addItem('Refresh Habits', 'refreshHabits')
    .addItem('Refresh Summary', 'refreshSummary')
    .addItem('Refresh Categories', 'refreshCategories')
    .addItem('Refresh Streaks', 'refreshStreaks')
    .addSeparator()
    .addItem('Setup Sheets', 'setupSheets')
    .addToUi();
}

/**
 * Setup all sheets with headers
 */
function setupSheets() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  
  // Create or get sheets
  createSheetIfNotExists(ss, 'Habits', [
    'sync_id', 'user_id', 'sync_date', 'sync_datetime', 'habit_name', 
    'habit_group', 'category', 'priority', 'priority_label', 'frequency',
    'score', 'grade', 'streak_length', 'is_numerical', 'habit_type',
    'record_date', 'value', 'completed', 'completed_text', 'day_of_week'
  ]);
  
  createSheetIfNotExists(ss, 'Summary', [
    'sync_date', 'sync_datetime', 'user_id', 'total_habits', 'active_habits',
    'average_score', 'min_score', 'max_score', 'numerical_habits', 'boolean_habits',
    'critical_count', 'high_count', 'medium_count', 'low_count',
    'grade_a_count', 'grade_b_count', 'grade_c_count', 'grade_d_count', 'grade_f_count'
  ]);
  
  createSheetIfNotExists(ss, 'Categories', [
    'sync_date', 'sync_datetime', 'category', 'habit_count',
    'average_score', 'min_score', 'max_score', 'grade',
    'total_streak_days', 'average_streak'
  ]);
  
  createSheetIfNotExists(ss, 'Streaks', [
    'sync_date', 'sync_datetime', 'habit_name', 'category',
    'priority', 'priority_label', 'streak_length', 'score'
  ]);
  
  SpreadsheetApp.getUi().alert('Setup complete! All sheets created with headers.');
}

/**
 * Helper to create sheet if it doesn't exist
 */
function createSheetIfNotExists(ss, name, headers) {
  let sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
    sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
    sheet.getRange(1, 1, 1, headers.length).setFontWeight('bold');
    sheet.setFrozenRows(1);
  }
}

/**
 * Fetch data from API
 */
function fetchData(type, params = {}) {
  let url = `${API_BASE_URL}?type=${type}`;
  
  // Add optional parameters
  for (const [key, value] of Object.entries(params)) {
    url += `&${key}=${encodeURIComponent(value)}`;
  }
  
  const response = UrlFetchApp.fetch(url);
  const json = JSON.parse(response.getContentText());
  return json.data || [];
}

/**
 * Refresh all datasets
 */
function refreshAllData() {
  refreshHabits();
  refreshSummary();
  refreshCategories();
  refreshStreaks();
  
  SpreadsheetApp.getUi().alert('All data refreshed successfully!');
}

/**
 * Refresh Habits sheet
 */
function refreshHabits() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName('Habits');
  
  if (!sheet) {
    SpreadsheetApp.getUi().alert('Please run setupSheets() first');
    return;
  }
  
  // Optional: Add date filter for last 30 days
  // const startDate = new Date();
  // startDate.setDate(startDate.getDate() - 30);
  // const data = fetchData('habits', { start_date: Utilities.formatDate(startDate, 'GMT', 'yyyy-MM-dd') });
  
  const data = fetchData('habits');
  
  // Clear existing data (keep headers)
  if (sheet.getLastRow() > 1) {
    sheet.deleteRows(2, sheet.getLastRow() - 1);
  }
  
  if (data.length === 0) return;
  
  // Prepare data for sheet
  const rows = data.map(row => [
    row.sync_id,
    row.user_id,
    row.sync_date,
    row.sync_datetime,
    row.habit_name,
    row.habit_group,
    row.category,
    row.priority,
    row.priority_label,
    row.frequency,
    row.score,
    row.grade,
    row.streak_length,
    row.is_numerical,
    row.habit_type,
    row.record_date,
    row.value,
    row.completed,
    row.completed_text,
    row.day_of_week
  ]);
  
  // Write to sheet
  sheet.getRange(2, 1, rows.length, rows[0].length).setValues(rows);
  
  // Add timestamp
  sheet.getRange('V1').setValue('Last Updated: ' + new Date().toLocaleString());
}

/**
 * Refresh Summary sheet
 */
function refreshSummary() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName('Summary');
  
  if (!sheet) {
    SpreadsheetApp.getUi().alert('Please run setupSheets() first');
    return;
  }
  
  const data = fetchData('summary');
  
  // Clear existing data (keep headers)
  if (sheet.getLastRow() > 1) {
    sheet.deleteRows(2, sheet.getLastRow() - 1);
  }
  
  if (data.length === 0) return;
  
  const rows = data.map(row => [
    row.sync_date,
    row.sync_datetime,
    row.user_id,
    row.total_habits,
    row.active_habits,
    row.average_score,
    row.min_score,
    row.max_score,
    row.numerical_habits,
    row.boolean_habits,
    row.critical_count,
    row.high_count,
    row.medium_count,
    row.low_count,
    row.grade_a_count,
    row.grade_b_count,
    row.grade_c_count,
    row.grade_d_count,
    row.grade_f_count
  ]);
  
  sheet.getRange(2, 1, rows.length, rows[0].length).setValues(rows);
  sheet.getRange('U1').setValue('Last Updated: ' + new Date().toLocaleString());
}

/**
 * Refresh Categories sheet
 */
function refreshCategories() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName('Categories');
  
  if (!sheet) {
    SpreadsheetApp.getUi().alert('Please run setupSheets() first');
    return;
  }
  
  const data = fetchData('categories');
  
  // Clear existing data (keep headers)
  if (sheet.getLastRow() > 1) {
    sheet.deleteRows(2, sheet.getLastRow() - 1);
  }
  
  if (data.length === 0) return;
  
  const rows = data.map(row => [
    row.sync_date,
    row.sync_datetime,
    row.category,
    row.habit_count,
    row.average_score,
    row.min_score,
    row.max_score,
    row.grade,
    row.total_streak_days,
    row.average_streak
  ]);
  
  sheet.getRange(2, 1, rows.length, rows[0].length).setValues(rows);
  sheet.getRange('L1').setValue('Last Updated: ' + new Date().toLocaleString());
}

/**
 * Refresh Streaks sheet
 */
function refreshStreaks() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName('Streaks');
  
  if (!sheet) {
    SpreadsheetApp.getUi().alert('Please run setupSheets() first');
    return;
  }
  
  const data = fetchData('streaks');
  
  // Clear existing data (keep headers)
  if (sheet.getLastRow() > 1) {
    sheet.deleteRows(2, sheet.getLastRow() - 1);
  }
  
  if (data.length === 0) return;
  
  const rows = data.map(row => [
    row.sync_date,
    row.sync_datetime,
    row.habit_name,
    row.category,
    row.priority,
    row.priority_label,
    row.streak_length,
    row.score
  ]);
  
  sheet.getRange(2, 1, rows.length, rows[0].length).setValues(rows);
  sheet.getRange('J1').setValue('Last Updated: ' + new Date().toLocaleString());
}

/**
 * Setup time-based trigger to auto-refresh daily
 * Run this once to enable automatic daily updates
 */
function setupDailyTrigger() {
  // Delete existing triggers
  const triggers = ScriptApp.getProjectTriggers();
  triggers.forEach(trigger => ScriptApp.deleteTrigger(trigger));
  
  // Create new trigger to run at 6 AM daily
  ScriptApp.newTrigger('refreshAllData')
    .timeBased()
    .atHour(6)
    .everyDays(1)
    .create();
  
  SpreadsheetApp.getUi().alert('Daily auto-refresh enabled at 6 AM');
}

/**
 * Setup hourly trigger for more frequent updates
 */
function setupHourlyTrigger() {
  // Delete existing triggers
  const triggers = ScriptApp.getProjectTriggers();
  triggers.forEach(trigger => ScriptApp.deleteTrigger(trigger));
  
  // Create new trigger to run every hour
  ScriptApp.newTrigger('refreshAllData')
    .timeBased()
    .everyHours(1)
    .create();
  
  SpreadsheetApp.getUi().alert('Hourly auto-refresh enabled');
}
