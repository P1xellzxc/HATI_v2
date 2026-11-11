---
title: "Real-use scenarios"
tags: ["hati/tests"]
---


Each scenario is a whole job a real group does. Expected money is calculated by hand (integer cents). The tap budget counts taps after the app is open, not typing. Implement each as an e2e check named `scenario: <title>` (see the [[hati-scenarios]] skill).

## 1. Dinner for four, one pays
Given a volume "Dinner" with You, Ana, Ben, Cy. When You add "Dinner" ₱120.00 paid by You, split equally. Then Ana, Ben and Cy each owe You ₱30.00; You are owed ₱90.00. Budget: 4 taps from the volume.

## 2. Someone leaves early
Given scenario 1. When Ben adds "Taxi" ₱45.00 paid by Ben, split equally among You, Ana and Ben (Cy unticked). Then net balances: You +75.00, Ana −45.00, Ben 0.00 (paid 45.00, owes 30.00 + 15.00), Cy −30.00 (sum 0). Budget: 6 taps (with up to 4 members the payer is one tap on the Paid by row; a picker that opens a list counts as 2).

## 3. Uneven shares
Given You, Ana, Ben. When Ana adds "Hotel" ₱300.00 paid by Ana, shares You 2, Ana 1, Ben 1. Then splits are 150.00 / 75.00 / 75.00; You owe Ana ₱150.00, Ben owes Ana ₱75.00.

## 4. Exact amounts
Given You and Ana. When You add "Groceries" ₱57.30, exact You 20.00, Ana 37.30. Then Ana owes You ₱37.30. A total that doesn't match (20.00 + 30.00) is blocked with a message saying ₱7.30 is left to assign.

## 5. Settle everything
Given scenario 2. When the user records every suggested payment from Settle up (two: Ana pays You ₱45.00, Cy pays You ₱30.00). Then every member shows "settled" and the balance panel says you're all settled. Budget: 2 taps per payment.

## 6. Two phones, one trip
Given phone A (Android) with volume "Bali" (You=Ana, Ben) and two expenses. When A shares the code and phone B (iPhone) imports it choosing "Ben", then B adds "Scooter" ₱80.00 paid by Ben split equally, shares back, and A imports with "Replace". Then both phones show the same three expenses and the same balances, each from their own point of view ("You").

## 7. First-time user
Given an empty app. When a new user creates a volume, adds two people and a first expense. Then it takes at most 10 taps, and the first screen told them what to do without reading more than one line.

## 8. Fix a mistake
Given scenario 1. When You edit "Dinner" to ₱100.00, then delete it. Then after the edit each owes ₱25.00; after deleting, everyone is settled.

## 9. Offline on the phone
Given the app installed and loaded once. When the phone goes offline and the user adds an expense and reopens the app. Then the expense is still there and fonts and icons look the same as online.

## 10. Rounding stays honest
Given You, Ana, Ben. When You add ₱100.00 split equally. Then shares are 33.34 / 33.33 / 33.33 and balances sum to exactly 0.00.

## 11. Join by invite link, two phones
Given phone A (Android, devices["Pixel 7"]) creates "Flat" with Ben and leaves its own name blank, and adds "Rent" 100.00 split equally. When A taps Share, gives its name (Ana) once, and sends the invite link; phone B (iPhone, devices["iPhone 13"], fresh storage) opens the link, picks "Ben" and taps Join, adds "Groceries" 40.00 split equally and sends its own link back; A opens that link and merges. Then both phones show the same 2 expenses, A (Ana) is owed 30.00 and B (Ben) owes 30.00, balances sum to 0, and B's picker named "Ana", never "Me". A opening its own older link again adds nothing. If B deletes the volume and opens the old link, it is offered as a new volume, not merged silently. Budget: B joins in 2 taps (opening the link 0, their name on the "Which member are you?" row 1, Join 1). Check: e2e block "R2-3" in web/tests/e2e.js.

## 12. Live sync, two phones, one offline
Given phone A (Android, devices["Pixel 7"]) creates "Beach" as Ana with Ben and adds "Rent" 100.00. When A taps Share (the volume gets a room and key, the invite is `<page>#join=<room>.<key>`), phone B (iPhone, devices["iPhone 13"], fresh storage) opens the link, picks "Ben" and joins; A adds "Taxi" 23.45 and B sees it without reloading; then both go offline, A adds "Lunch" 30.00 and B adds "Snacks" 12.10, and both reconnect (B first, so A's write gets a 409 and merges). Then both phones hold the same 4 expenses in the same order, Ana and Ben's balances are opposite to the cent and sum to 0, both say "Synced", and no request body or URL holds a name, title, amount or the key. A wrong key, or a blob A can't decrypt, changes nothing on the phone. Check: e2e block "Criterion 6 ([[hati-sync]])" (S6-1 … S6-11) in web/tests/e2e.js, against a mocked server (Playwright `route` + `routeWebSocket`).

## 13. Change the look, no bottom bar
Given the app on the hub (there is no bottom bar). When the user taps the gear at the top right of the hub, picks a color theme and Dark, then taps Back (or the phone's Back). Then Settings opened in 1 tap (`#prefs`, the page wipes left), the theme applies at once, Back lands on the hub (the wipe goes right) without leaving the app, and opening `#prefs` directly still shows Settings with Back to the hub. From inside a volume Settings is Back then the gear (2 taps), by decision. Content scrolls to 24px plus the bottom safe area from the screen edge, with no strip under it, at 320/360/390 px in both themes. Check: e2e block "NB" in web/tests/e2e.js.

## 14. Tidy the shelf: edit, archive, delete a volume from the hub
Given the hub with "Bali" (Alex, Mira, Rio, Zed; Zed in no expense) and the example volume. When the user presses and holds the Bali card (~450 ms, the card inks in from the left and the phone buzzes), the sheet offers Edit volume, Archive, Delete volume and Cancel; lifting the finger does not open the volume. Edit renames it and Mira, changes type and currency, removes Zed (×) and adds Noor and Ola in one Save. Archive moves it to a closed "Archived (1)" shelf at the bottom; opened from there it is read-only with an Unarchive button. Delete asks "Delete volume?", then a toast offers Undo for 6 s; after that a reload, a restored backup or a live change from another phone does not bring it back (opening an invite to it again does). On another phone in the same live volume, archiving sends nothing and the volume stays unarchived; deleting on one phone leaves the other's copy intact. Without a hold: right-click, Shift+F10, the menu key, or the "Options for Bali" button (screen reader, Tab). Budget: options in 1 hold; archive in 2 taps. Check: e2e block "LP" in web/tests/e2e.js.

## 15. Pick a category in one tap
Given a new expense on a 320 px phone. Then the 12 categories (Eating out, Groceries, Drinks, Transport, Fuel, Stay & rent, Fun, Shopping, Bills, Health, Gifts, Other) are all on screen as 3 rows of 4 (4 rows of 3 at 320 px) with no label cut off, one tap picks one, and the next new expense in that volume starts on the category used last. Expenses saved with an id this version doesn't know show as Other. Check: e2e block "CAT" in web/tests/e2e.js.

## 16. Pick a payer, a person, a currency without the phone's dropdown
Given a couple volume (You, Ivy) on a 320 px phone in either theme. When You add an expense Ivy paid, "Paid by" is a row "You | Ivy" and Ivy is one tap; Settle up shows From and To as the same rows; a new volume's Type is a row where its four words fit (a button that opens a list where they don't, as at 320 px) and Currency is a button that opens an ink list of the 10 currencies. A shared volume's "What to do" is Merge | Replace | Keep both, and "Which member are you?" starts on no one until you pick. Every picker works with arrows, Enter and Esc and says its name and choice to a screen reader. Then the saved payer, from/to, type, currency, import mode and member are the same values as before. Budget: payer 1 tap (2–4 members), currency 2 taps. Check: e2e block "D0" in web/tests/e2e.js.

## 16. The same meal again (repeat entry)
Given "Bebe" (You and Ivy) with past expenses "Chowking" (twice, Ivy paid, Eating out, split equally) and others. When You add Chowking again, ₱412.00, paid by Ivy. Then the description suggestion fills description, category and split in one tap, the amount is typed (or filled by tapping "Last time ₱352.35", never on its own), and the new expense splits ₱206.00 each. Budget: 4 taps (Add expense, the Chowking suggestion, Ivy, Save). e2e: "D1 scenario 16".

## 17. Fix the dates of imported expenses (N5)
Given "Bebetime" (You and Ivy) where every imported expense is dated today. When You open History, tap Set dates, tap 6 rows, pick Oct 15 and tap Set date. Then those 6 expenses show Oct 15 after a reload, the rest keep their date, amounts, splits and balances are byte-identical, and the toast "Dates changed on 6 expenses." offers Undo. Budget: 1 + 6 + 2 (date) + 1 = 10 taps, no expense form opened. Check: e2e "N5: …".

## 18. An expense from yesterday (N5)
Given any volume. When You add an expense and tap Yesterday under Date. Then it is saved with yesterday's date. Budget: one tap after the amount. Check: e2e "N5 scenario: …".

## 19. Paste a receipt total (D9)
Given a new expense in "Bebetime" and receipt text copied from a photo (iPhone Live Text or Google Lens): "JOLLIBEE FOODS CORPORATION … TOTAL 334.13 / CASH 500.00 / CHANGE 165.87 / VAT 12% 35.80". When You paste it into the amount. Then the amount is 334.13 (the TOTAL line, never CASH or CHANGE), the empty description becomes "JOLLIBEE FOODS CORPORATION", and a quiet line says "From paste: TOTAL ₱334.13". Text with no total line and several amounts shows up to 3 of them as buttons ("Which one is the total?") and fills nothing until one is tapped; text with no amount says "No amount found in what you pasted." and changes nothing; a plain number pastes as before. Budget: 1 paste (0 extra taps; 1 tap when it has to ask). Check: e2e "D9 …" and the parser table `web/tests/receipt.js` (28 receipts).

## 20. Undo a wrong delete or edit (D2)
Given "Bebetime" with "House of Bagnet" ₱334.13 and a ₱500.00 payment to Ivy. When You delete House of Bagnet by mistake and tap Undo on the toast (or Tab to it and press Enter). Then it is back with the same amount, payer, split, date and category (under a new id, so Ivy's phone, which already has the delete, takes it back too), and balances are as before to the cent. The same Undo follows "Expense updated", "Payment recorded" and "Payment deleted"; "Undone." confirms. Budget: 1 tap. Check: e2e "D2 …".

## 21. Yesterday's GCash payment (D3)
Given "Bebetime" where You owe Ivy. When You tap Settle up, tap Yesterday under Date, type "GCash" in How (optional) and tap Record payment. Then the payment is saved with yesterday's date and `how: "GCash"`, History shows "Payment · via GCash" and "Yesterday", editing it opens on that date and how, and the balances are the same as without a how. The how survives a reload, a backup, a share code and a merge with a phone on an older version (which drops it without harm). Budget: 4 taps (Settle up, Yesterday, How, Record payment). Check: e2e "D3 …".


Run by [[hati-scenarios]]. Back to [[HATI Brain]]
