# Pla d'implementació: Navegació al Detall del Dia

Aquest document descriu els passos per implementar la navegació des del calendari cap a una pantalla de detall d'un dia específic quan l'usuari el selecciona.

## 1. Definició de la Clau de Navegació
Afegir una nova `NavKey` serialitzable al fitxer `CalendarNavigation.kt`. Aquesta clau inclourà la data del dia seleccionat com a paràmetre.

- **Clau:** `DayDetailNavKey(val date: LocalDate)`

## 2. Creació de la Pantalla de Detall (Placeholder)
Crear un nou fitxer `DayDetailScreen.kt` a la carpeta de `screens` del mòdul de calendari.
- Implementar una estructura bàsica amb un `Scaffold` i un `TopBar`.
- Mostrar la data rebuda com a títol o en un text central per confirmar que la navegació funciona i rep les dades correctament.

## 3. Configuració de l'Entrada de Navegació
Registrar la nova pantalla al `EntryProviderScope` dins de `CalendarNavigation.kt`:
- Afegir `entry<DayDetailNavKey>`.
- Passar el paràmetre `date` de la clau a la pantalla `DayDetailScreen`.

## 4. Actualització del CalendarScreen
Modificar `CalendarScreen` per acceptar un nou callback d'esdeveniment:
- **Callback:** `onDayClick: (LocalDate) -> Unit`
- Connectar aquest callback amb el component del calendari (presumiblement `CalendarContent` o similar) perquè es dispari quan l'usuari premi un dia.

## 5. Connexió de la Lògica de Navegació
A `CalendarNavigation.kt`, actualitzar la crida a `CalendarScreen` per definir el comportament del `onDayClick`:
- En prémer un dia, cridar `navigator.navigate(DayDetailNavKey(date))`.

## 6. Verificació
- Comprovar que en polsar qualsevol dia del calendari, l'app navega a la nova pantalla.
- Verificar que la data mostrada a la pantalla de detall coincideix amb la data premuda.
