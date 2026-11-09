# HATI² real-use scenarios

Each scenario is a whole job a real group does. Expected money is calculated by hand (integer cents). The tap budget counts taps after the app is open, not typing. Implement each as an e2e check named `scenario: <title>` (see the `hati-scenarios` skill).

## 1. Dinner for four, one pays
Given a volume "Dinner" with You, Ana, Ben, Cy. When You add "Dinner" ₱120.00 paid by You, split equally. Then Ana, Ben and Cy each owe You ₱30.00; You are owed ₱90.00. Budget: 4 taps from the volume.

## 2. Someone leaves early
Given scenario 1. When Ben adds "Taxi" ₱45.00 paid by Ben, split equally among You, Ana and Ben (Cy unticked). Then net balances: You +75.00, Ana −45.00, Ben 0.00 (paid 45.00, owes 30.00 + 15.00), Cy −30.00 (sum 0). Budget: 7 taps (a picker counts as 2 taps).

## 3. Uneven shares
Given You, Ana, Ben. When Ana adds "Hotel" ₱300.00 paid by Ana, shares You 2, Ana 1, Ben 1. Then splits are 150.00 / 75.00 / 75.00; You owe Ana ₱150.00, Ben owes Ana ₱75.00.

## 4. Exact amounts
Given You and Ana. When You add "Groceries" ₱57.30, exact You 20.00, Ana 37.30. Then Ana owes You ₱37.30. A total that doesn't match (20.00 + 30.00) is blocked with a message saying ₱7.30 is left to assign.

## 5. Settle everything
Given scenario 2. When the user records every suggested payment from Settle up (two: Ana pays You ₱45.00, Cy pays You ₱30.00). Then every member shows "settled" and the balance panel says you're all settled. Budget: 2 taps per payment.

## 6. Two phones, one trip
Given phone A (Android) with volume "Bali" (You=Ana, Ben) and two expenses. When A shares the code and phone B (iPhone) imports it choosing "Ben", then B adds "Scooter" ₱80.00 paid by Ben split equally, shares back, and A imports with "Replace my copy". Then both phones show the same three expenses and the same balances, each from their own point of view ("You").

## 7. First-time user
Given an empty app. When a new user creates a volume, adds two people and a first expense. Then it takes at most 10 taps, and the first screen told them what to do without reading more than one line.

## 8. Fix a mistake
Given scenario 1. When You edit "Dinner" to ₱100.00, then delete it. Then after the edit each owes ₱25.00; after deleting, everyone is settled.

## 9. Offline on the phone
Given the app installed and loaded once. When the phone goes offline and the user adds an expense and reopens the app. Then the expense is still there and fonts and icons look the same as online.

## 10. Rounding stays honest
Given You, Ana, Ben. When You add ₱100.00 split equally. Then shares are 33.34 / 33.33 / 33.33 and balances sum to exactly 0.00.

## 11. Join by invite link, two phones
Given phone A (Android, devices["Pixel 7"]) creates "Flat" with Ben and leaves its own name blank, and adds "Rent" 100.00 split equally. When A taps Share, gives its name (Ana) once, and sends the invite link; phone B (iPhone, devices["iPhone 13"], fresh storage) opens the link, picks "Ben" and taps Join, adds "Groceries" 40.00 split equally and sends its own link back; A opens that link and merges. Then both phones show the same 2 expenses, A (Ana) is owed 30.00 and B (Ben) owes 30.00, balances sum to 0, and B's picker named "Ana", never "Me". A opening its own older link again adds nothing. If B deletes the volume and opens the old link, it is offered as a new volume, not merged silently. Budget: B joins in 3 taps (opening the link 0, picker 2, Join 1). Check: e2e block "R2-3" in web/tests/e2e.js.

## 12. Live sync, two phones, one offline
Given phone A (Android, devices["Pixel 7"]) creates "Beach" as Ana with Ben and adds "Rent" 100.00. When A taps Share (the volume gets a room and key, the invite is `<page>#join=<room>.<key>`), phone B (iPhone, devices["iPhone 13"], fresh storage) opens the link, picks "Ben" and joins; A adds "Taxi" 23.45 and B sees it without reloading; then both go offline, A adds "Lunch" 30.00 and B adds "Snacks" 12.10, and both reconnect (B first, so A's write gets a 409 and merges). Then both phones hold the same 4 expenses in the same order, Ana and Ben's balances are opposite to the cent and sum to 0, both say "Synced", and no request body or URL holds a name, title, amount or the key. A wrong key, or a blob A can't decrypt, changes nothing on the phone. Check: e2e block "Criterion 6 (hati-sync)" (S6-1 … S6-11) in web/tests/e2e.js, against a mocked server (Playwright `route` + `routeWebSocket`).

## 13. Change the look, no bottom bar
Given the app on the hub (there is no bottom bar). When the user taps the gear at the top right of the hub, picks a color theme and Dark, then taps Back (or the phone's Back). Then Settings opened in 1 tap (`#prefs`, the page wipes left), the theme applies at once, Back lands on the hub (the wipe goes right) without leaving the app, and opening `#prefs` directly still shows Settings with Back to the hub. From inside a volume Settings is Back then the gear (2 taps), by decision. Content scrolls to 24px plus the bottom safe area from the screen edge, with no strip under it, at 320/360/390 px in both themes. Check: e2e block "NB" in web/tests/e2e.js.
