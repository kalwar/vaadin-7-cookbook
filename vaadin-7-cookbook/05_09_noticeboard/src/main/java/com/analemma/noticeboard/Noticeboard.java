package com.analemma.noticeboard;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class Noticeboard extends VerticalLayout {
  private static List<Note> notes = new ArrayList<>();
  private final List<Window> windows = new ArrayList<>();
  private static int userCount;
  private final int userId;
  private static int noteId = 1;

  private static final int UPDATE_INTERVAL = 2000;

  public Noticeboard() {
    userId = userCount++;
    setSpacing(true);
    setMargin(true);
    addComponent(new Label("Logged as an User " + userId));
    final Button addNoteButton = new Button("Add note");

    addNoteButton.addClickListener(new ClickListener() {
      @Override
      public void buttonClick(final ClickEvent event) {
        final Note note = new Note(noteId++);
        notes.add(note);
        final Window window = createWindow(note);
        windows.add(window);
        UI.getCurrent().addWindow(window);
      }
    });

    addComponent(addNoteButton);
    new NoticeboardUpdater().start();
  }

  private Window createWindow(final Note note) {
    final Window window = new Window(note.getCaption());
    final Layout layout = new VerticalLayout();
    layout.addComponent(createContentNote(note, window));
    window.setContent(layout);
    window.setResizable(false);
    window.setPositionX(note.getPositionX());
    window.setPositionY(note.getPositionY());
    window.setData(note);
    window.addBlurListener(createBlurListener(window));
    window.addFocusListener(createFocusListener(window));
    return window;
  }

  private TextArea createContentNote(final Note note, final Window window) {
    final TextArea contentNote = new TextArea();
    contentNote.setSizeFull();
    contentNote.setValue(note.getText());
    contentNote.setImmediate(true);
    contentNote.setTextChangeEventMode(TextChangeEventMode.EAGER);
    contentNote.addBlurListener(createBlurListener(window));
    contentNote.addFocusListener(createFocusListener(window));
    contentNote.addTextChangeListener(new TextChangeListener() {
      @Override
      public void textChange(final TextChangeEvent event) {
        note.setText(event.getText());
      }
    });
    return contentNote;
  }

  private BlurListener createBlurListener(final Window window) {
    return new BlurListener() {
      @Override
      public void blur(final BlurEvent event) {
        unlockNote(window);
      }
    };
  }

  private FocusListener createFocusListener(final Window window) {
    return new FocusListener() {
      @Override
      public void focus(final FocusEvent event) {
        lockNote(window);
      }
    };
  }

  private void lockNote(final Window window) {
    final Note note = (Note) window.getData();
    note.setLockedByUser(userId);
    final String caption = "LOCKED by User " + userId;
    note.setCaption(caption);
    window.setCaption(caption);
  }

  private void unlockNote(final Window window) {
    final Note note = (Note) window.getData();
    note.setLockedByUser(-1);
    note.setPositionX(window.getPositionX());
    note.setPositionY(window.getPositionY());
    note.setCaption("Note " + note.getId());
    window.setCaption("Note " + note.getId());
  }

  private void updateNoticeboard() {
    for (final Note note : notes) {
      Window window = getWindow(note);
      updateTextArea(window, note);

      if (window == null) {
        window = createWindow(note);
        windows.add(window);
        UI.getCurrent().addWindow(window);
      }

      if (note.getLockedByUser() > -1 && note.getLockedByUser() != userId) {
        window.setEnabled(false);
      } else {
        window.setEnabled(true);
      }

      if (note.getLockedByUser() == userId) {
        final Note focusedNote = (Note) window.getData();
        focusedNote.setPositionX(window.getPositionX());
        focusedNote.setPositionY(window.getPositionY());
        focusedNote.setCaption(window.getCaption());
      } else {
        window.setPositionX(note.getPositionX());
        window.setPositionY(note.getPositionY());
        window.setCaption(note.getCaption());
      }
    }
  }

  private void updateTextArea(final Window window, final Note note) {
    if (window == null) {
      return;
    }
    final Layout layout = (Layout) window.getContent();
    final TextArea area = (TextArea) layout.iterator().next();
    area.setValue(note.getText());
  }

  private Window getWindow(final Note note) {
    for (final Window window : windows) {
      if (window.getData().equals(note)) {
        return window;
      }
    }
    return null;
  }

  public class NoticeboardUpdater extends Thread {
    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(UPDATE_INTERVAL);
        } catch (final InterruptedException e) {
          e.printStackTrace();
        }

        getUI().access(new Runnable() {

          @Override
          public void run() {
            updateNoticeboard();
          }
        });

      }
    }
  }

}
