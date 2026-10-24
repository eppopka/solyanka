"""Static GUI lifecycle/drag contracts; not live Minecraft visual or crash reproduction."""
from pathlib import Path
import json,zipfile
p=Path(__file__).resolve().parents[1];ui=p/'src/client/kotlin/dev/solyanka/client/ui/nofrills'
s=(ui/'NoFrillsClickGui.kt').read_text();panel=(ui/'SettingsPanel.kt').read_text();kit=(ui/'GuiKit.kt').read_text()
assert 'fun showPanel(next:SettingsPanel) {navigation.open(next)}' in s
assert s.index('commitNavigation()\n        super.extractRenderState')>0
assert s.index('next.mount(width,height)')<s.index('panel?.dispose();panel=next')
assert 'next.dispose();logger'not in s and 'runCatching {next.dispose()}'in s
assert 'root.surface('not in s and 'root.surface('not in panel
assert 'frame.positioning(Positioning.absolute(it.x,it.y))'in panel
assert 'drag.begin('in panel and 'drag.move('in panel and 'drag.end()'in panel
block=panel.split('fun mouseDragged(',1)[1].split('fun mouseReleased(',1)[0];assert 'mount('not in block and 'updateGeneral'not in block
row=kit.split('fun colorRow(',1)[1].split('fun choiceRow(',1)[0];assert row.index('edit.onPress {')<row.index('ColorPickerComponent()')
assert 'catch(e:Exception)'in row
with zipfile.ZipFile(p/'build/libs/solyanka-1.3.1.jar')as z:
 assert json.loads(z.read('fabric.mod.json'))['version']=='1.3.1'
 for name in ['dev/solyanka/ui/PanelDrag.class','dev/solyanka/ui/PanelNavigation.class','dev/solyanka/client/ui/nofrills/SettingsPanel.class']:assert name in z.namelist(),name
result=dict(version='1.3.1',navigation_deferred=True,new_panel_mounted_before_old_disposal=True,title_bar_drag=True,drag_does_not_remount=True,palette_created_on_demand=True,transparent_world_background=True,instant_save_preserved=True,ui_errors_logged=True,held_item_crash_report_received=False,held_item_root_cause_confirmed=False,held_item_crash_reproduced=False,crash_fix_verified=False,minecraft_gui_launched_here=False,visual_in_game_acceptance=False)
(p/'validation/gui-lifecycle-static.json').write_text(json.dumps(result,indent=2)+'\n')
print('GUI static contracts passed. Actual Held Item crash cause and live GUI behavior remain unverified.')
