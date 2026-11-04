# HATI² real-use scenarios

Each scenario is a whole job a real group does. Expected money is calculated by hand (integer cents). The tap budget counts taps after the app is open, not typing. Implement each as an e2e check named `scenario: <title>` (see the `hati-scenarios` skill).

## 1. Dinner for four, one pays
Given a volume "Dinner" with You, Ana, Ben, Cy. When You add "Dinner" ₱120.00 paid by You, split equally. Then Ana, Ben and Cy each owe You ₱30.00; You are owed ₱90.00. Budget: 4 taps from the volume.

## 2. Someone leaves early
Given scenario 1. When Ben adds "Taxi" ₱45.00 paid by Ben, split equally among You, Ana and Ben (Cy unticked). Then net balances: You +75.00, Ana −45.00, Ben 0.00 (paid 45.00, owes 30.00 + 15.00), Cy −30.00 (sum 0). Budget: 6 taps (a picker counts as 2 taps).

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
