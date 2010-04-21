package org.literacybridge.acm.ui.player;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;
import org.literacybridge.acm.player.SimpleSoundPlayer;

public class PlayerUI extends Container {

	private static final long serialVersionUID = -1827563460140622507L;

	// Player (will run in a different thread!)
	private SimpleSoundPlayer player = new SimpleSoundPlayer();
	
	// resources
	ImageIcon imagePlay = new ImageIcon(getClass().getResource("/play-24px.png"));
	ImageIcon imageRight = new ImageIcon(getClass().getResource("/back-24px.png"));
	ImageIcon imageLeft = new ImageIcon(getClass().getResource("/forward-24px.png"));
	ImageIcon imagePause = new ImageIcon(getClass().getResource("/pause-24px.png"));
	
    // Variables declaration - do not modify
    private javax.swing.JButton backwardBtn;
    private javax.swing.JButton forwardBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSlider jSlider1;
    private javax.swing.JButton playBtn;
    private javax.swing.JLabel playedTimeLbl;
    private javax.swing.JLabel remainingTimeLbl;
    private javax.swing.JTextField seachTF;
    private javax.swing.JLabel titleInfoLbl;
    // End of variables declaration
	
	
	public PlayerUI() {

		initComponents();
		// testing
		String audioFile = "/Volumes/MAC_HOME/USERS/coder/Projects/talkingbook/acm/TestData/testWav.wav";
		initPlayer(audioFile);
	}
	
	

	private boolean initPlayer(String audioFilePath) {
		File audioFile = new File(audioFilePath);
		player.setClip(audioFile);		
		return true;
	}
	
	
	private String secondsToTimeString(int seconds) {
		final int SECONDS_PER_MINUTE = 60;
		return String.format("%d:%02d", 
				  seconds / SECONDS_PER_MINUTE, 
				  seconds % SECONDS_PER_MINUTE);
	}
	
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        backwardBtn = new javax.swing.JButton();
        playBtn = new javax.swing.JButton();
        forwardBtn = new javax.swing.JButton();
        seachTF = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jSlider1 = new javax.swing.JSlider();
        playedTimeLbl = new javax.swing.JLabel();
        remainingTimeLbl = new javax.swing.JLabel();
        titleInfoLbl = new javax.swing.JLabel();

        backwardBtn.setIcon(imageLeft); // NOI18N
        backwardBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));

        playBtn.setIcon(imagePlay); // NOI18N
        playBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));
        playBtn.setPreferredSize(new java.awt.Dimension(44, 44));

        forwardBtn.setIcon(imageRight); // NOI18N
        forwardBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));

        seachTF.setText("Search");

        jLabel1.setIcon(imagePause); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel1.setMinimumSize(new java.awt.Dimension(300, 100));

        jSlider1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        playedTimeLbl.setText("00:00:00");
        playedTimeLbl.setName("null"); // NOI18N

        remainingTimeLbl.setText("00:00:00");

        titleInfoLbl.setText("jLabel1");
       
        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout .LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(playedTimeLbl)
                .addPreferredGap(LayoutStyle.UNRELATED)
                .add(jPanel1Layout.createParallelGroup(GroupLayout.CENTER)
                    .add(jSlider1, GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE)
                    .add(titleInfoLbl, GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE))
                .addPreferredGap(LayoutStyle.RELATED)
                .add(remainingTimeLbl))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(titleInfoLbl)
                .addPreferredGap(LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(GroupLayout.LEADING)
                    .add(playedTimeLbl, GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                    .add(remainingTimeLbl, GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                    .add(jSlider1, GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE))
                .addContainerGap())
        );

        playedTimeLbl.getAccessibleContext().setAccessibleName("");

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(backwardBtn)
                .addPreferredGap(LayoutStyle.UNRELATED)
                .add(playBtn, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.UNRELATED)
                .add(forwardBtn)
                .add(18, 18, 18)
                .add(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.UNRELATED)
                .add(jLabel1)
                .add(2, 2, 2)
                .add(seachTF, GroupLayout.PREFERRED_SIZE, 151, GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(42, 42, 42)
                        .add(layout.createParallelGroup(GroupLayout.LEADING, false)
                            .add(forwardBtn, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(playBtn, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(backwardBtn, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(seachTF, GroupLayout.DEFAULT_SIZE, 44, Short.MAX_VALUE)
                            .add(jLabel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel1, GroupLayout.PREFERRED_SIZE, 88, GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        layout.linkSize(new java.awt.Component[] {forwardBtn, jLabel1, seachTF}, GroupLayout.VERTICAL);

        playBtn.getAccessibleContext().setAccessibleName("Play");
    }// </editor-fold>

	
	
	/*
	 * Testing only
	 */	
	public static void main(String[] args) {
		JFrame frame = new JFrame("TestPlayer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new PlayerUI(), BorderLayout.CENTER);
		frame.setLocation(300, 300);
		frame.pack();
		frame.setVisible(true);
	}
}
